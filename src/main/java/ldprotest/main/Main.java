/*
 * This File is Part of LDProtest
 * Copyright (C) 2021 Covid Anti Hysterics League
 *
 * This program is free software: you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Lesser Public License for more
 * details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/
package ldprotest.main;

import com.mongodb.MongoException;
import java.io.File;
import java.io.FileInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ldprotest.db.SetupDatabase;
import ldprotest.server.infra.Server;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.Semaphore;
import ldprotest.config.AppConfig;
import ldprotest.config.CmdLineArgs;
import ldprotest.config.ConfigFile;
import ldprotest.config.DefaultConfig;
import ldprotest.db.MainDatabase;
import ldprotest.main.AppLogging.ResourceType;
import ldprotest.tasks.PeriodicTaskManager;
import ldprotest.util.ErrorCode;

import ldprotest.server.auth.webtoken.UserTokens;
import ldprotest.tasks.ProtestVacuum;
import ldprotest.tasks.SessionVacuum;
import ldprotest.util.Result;

public class Main {

    private static AppConfig ARGS;

    public static final int SERVER_SHUTDOWN_HANDLER_PRIORITY = Integer.MAX_VALUE/2;

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    private final static PriorityBlockingQueue<ShutdownHandler> EXIT_HANDLERS = new PriorityBlockingQueue<>(
        10, Comparator.comparingInt(ShutdownHandler::priority)
    );
    private final static Set<Integer> USED_PRIORITIES = new HashSet<>();

    private static final Semaphore STOP_REQUEST_SEMAPHORE = new Semaphore(0, false);
    private static final Semaphore EXIT_COMPLETE_SEMAPHORE = new Semaphore(0, false);

    private static final String DEFAULT_LOGBACK_CONFIG = "/config/logback.xml";

    private static int exitCode = 0;

    public static synchronized void registerShutdownHandler(ShutdownHandler h) {
        Integer priority = h.priority();

        if(USED_PRIORITIES.contains(priority)) {
            LOGGER.error("Attempted to set two exit handlers with priority of {}", priority);
            throw new IllegalStateException("Priority already in use");
        }

        EXIT_HANDLERS.add(h);
        USED_PRIORITIES.add(priority);
    }

    public static void registerShutdownHandler(int prio, Runnable h) {
        registerShutdownHandler(new ShutdownHandler() {
            @Override
            public int priority() {
                return prio;
            }

            @Override
            public void run() {
                h.run();
            }
        });
    }

    public static void requestShutdown() {
        requestShutdown(0);
    }

    public static void requestShutdown(int exitCode) {
        Main.exitCode = exitCode;
        STOP_REQUEST_SEMAPHORE.release();
    }

    public static AppConfig args() {
        return ARGS;
    }

    public static void main(String[] args) {
        AppConfig.Builder configBuilder = DefaultConfig.defaultBuilder();
        Result<Integer, AppConfig.Builder> cmdLineArgs = CmdLineArgs.parse(configBuilder, args);

        if(cmdLineArgs.isFailure()) {
            System.exit(cmdLineArgs.failureReason());
        } else {
            if(cmdLineArgs.result().helpRequested.get()) {
                System.exit(0);
            }

            Result<Integer, AppConfig.Builder> loadConfigResult = loadConfig(configBuilder);
            if(loadConfigResult.isFailure()) {
                System.exit(loadConfigResult.failureReason());
            }

            ARGS = cmdLineArgs.result().build();
        }

        try {
            if(ARGS.logbackPath.isEmpty()) {
                AppLogging.setLogbackConfig(DEFAULT_LOGBACK_CONFIG, ResourceType.ON_CLASSPATH);
            } else {
                AppLogging.setLogbackConfig(ARGS.logbackPath, ResourceType.ON_FILE_SYSTEM);
            }
        }
        catch(IOException e) {
            LOGGER.error("Error setting logback configuration.");
            System.exit(-1);
        }

        LOGGER.info("Lockdown Protest Server Initializing");

        setupShutdownHandling();
        registerShutdownHandler(() -> LOGGER.info("Server Exited Cleanly..."));
        registerShutdownHandler(SERVER_SHUTDOWN_HANDLER_PRIORITY, () -> Server.stop());

        try {
            startComponents();
        } catch(Throwable e) {
            LOGGER.error("Exiting due to fatal error in server startup.", e);
            requestShutdown(-1);
        }

        try {
            STOP_REQUEST_SEMAPHORE.acquire();
        } catch(InterruptedException e) {
            LOGGER.info("Main thread interrupted, exiting.");
        }

        runShutdownHandlers();
        EXIT_COMPLETE_SEMAPHORE.release();
        System.exit(Main.exitCode);
    }

    private static Result<Integer, AppConfig.Builder> loadConfig(AppConfig.Builder builder) {
        String path = builder.configFilePath.get();

        if(path.isEmpty()) {
            return Result.success(builder);
        }

        File file = new File(path);

        try(FileInputStream stream = new FileInputStream(file)) {
            Result<String, AppConfig.Builder> configLoadResult = ConfigFile.parse(builder, stream);

            if(configLoadResult.isFailure()) {
                LOGGER.error("Error reading config file: {}", configLoadResult.failureReason());
                return Result.failure(-1);
            } else {
                return Result.success(builder);
            }
        } catch (IOException ex) {
            LOGGER.error("Unable to read config file: {}", ex.getMessage());
            return Result.failure(-1);
        }
    }

    private static void startComponents() throws IOException {
        MainDatabase.connect(ARGS.mongoConnect);
        waitForDatabase();

        LOGGER.info("Database Version {}", SetupDatabase.setup(MainDatabase.database()));

        UserTokens.init();
        startPeriodicTasks();

        Server.start();
    }

    private static void startPeriodicTasks() {
        PeriodicTaskManager.start();
        SessionVacuum.register();
        ProtestVacuum.register();
    }

    private static void waitForDatabase() {
        ErrorCode<MongoException> e = MainDatabase.testConnection();

        if(e.failed()) {
            LOGGER.warn("No database connection at startup: {}", e);
            LOGGER.warn("Waiting for database to connect before continuing...");

            while(!hasShutdownBeenRequested() && e.failed()) {
                e = MainDatabase.testConnection();
            }

            if(e.failed()) {
                LOGGER.info("Shutdown requested before database connected.");
            } else {
                LOGGER.info("Database connected. Continuing with startup.");
            }
        }
    }

    private static void setupShutdownHandling() {
        Runtime.getRuntime().addShutdownHook(
            new Thread(() -> {
                requestShutdown();
                try {
                    EXIT_COMPLETE_SEMAPHORE.acquire();
                } catch (InterruptedException e) {
                    LOGGER.error("Unexpected interruption during shutdown");
                }
            })
        );
    }

    private static void registerShutdownHandler(Runnable h) {
        registerShutdownHandler(Integer.MAX_VALUE, h);
    }

    private static void runShutdownHandlers() {
        ShutdownHandler h;
        while( (h = EXIT_HANDLERS.poll()) != null) {
            h.run();
        }
    }

    private static boolean hasShutdownBeenRequested() {
        return STOP_REQUEST_SEMAPHORE.availablePermits() != 0;
    }
}
