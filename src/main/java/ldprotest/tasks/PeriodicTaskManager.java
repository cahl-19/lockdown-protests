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
package ldprotest.tasks;

import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import ldprotest.main.Main;
import ldprotest.main.ShutdownHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PeriodicTaskManager {

    private final static Logger LOGGER = LoggerFactory.getLogger(PeriodicTaskManager.class);
    public static int SHUTDOWN_HANDLER_PRIORITY = Main.SERVER_SHUTDOWN_HANDLER_PRIORITY + 16;

    private static final int THREADS = 2;
    private static PeriodicTaskManager INSTANCE;

    private final ScheduledExecutorService executor;
    private final ShutdownHandler shutdownHandler;
    private final List<ScheduledTaskDescriptor> tasks;
    private final ShutdownSignal shutdownSignal;

    private PeriodicTaskManager() {
        tasks = new CopyOnWriteArrayList<>();
        executor = new ScheduledThreadPoolExecutor(THREADS);
        shutdownHandler = buildShutdownHandler();
        shutdownSignal = new ShutdownSignal();
    }

    private void registerTaskMethod(
        PeriodicTask task, long delay, long period, TimeUnit timeUnit, boolean mayInterrupt
    ) {
        ScheduledFuture<?> future = executor.scheduleAtFixedRate( () -> {
            try {
                task.runTask(shutdownSignal);
            } catch(Throwable ex) {
                LOGGER.error("Uncaught exception thrown in periodic task", ex);
            }
        }, delay, period, timeUnit);
        tasks.add(new ScheduledTaskDescriptor(future, mayInterrupt));
    }

    public static void start() {
        if(INSTANCE != null) {
            throw new IllegalStateException("Periodic task manager was already started");
        }

        INSTANCE = new PeriodicTaskManager();

        Main.registerShutdownHandler(INSTANCE.shutdownHandler);
    }

    public static void registerTask(
        long delay, long period, TimeUnit timeUnit, boolean mayInterrupt, PeriodicTask task
    ) {
        INSTANCE.registerTaskMethod(task, delay, period, timeUnit, mayInterrupt);
    }

    private ShutdownHandler buildShutdownHandler() {
        return new ShutdownHandler() {
            @Override
            public int priority() {
                return SHUTDOWN_HANDLER_PRIORITY;
            }
            @Override
            public void run() {
                shutdownSignal.requestShutdown();

                for(ScheduledTaskDescriptor task: tasks) {
                    task.cancel();
                }
                for(ScheduledTaskDescriptor task: tasks) {
                    task.join();
                }
            }
        };
    }

    private static final class ScheduledTaskDescriptor {
        private final ScheduledFuture<?> future;
        private final boolean mayInterrupt;

        public ScheduledTaskDescriptor(ScheduledFuture<?> future, boolean mayInterrupt) {
            this.future = future;
            this.mayInterrupt = mayInterrupt;
        }

        public void cancel() {
            future.cancel(mayInterrupt);
        }

        public void join() {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException | CancellationException ex) {
                /*note: it is actually the epxected case for a scheduled  future to throw an exception on termination*/
            }

            if(!future.isDone()) {
                LOGGER.warn("Periodic task returning not done, despite get returning.");
            }
        }
    }

    public static final class ShutdownSignal {

        private final AtomicBoolean exitRequested;

        private ShutdownSignal() {
            exitRequested = new AtomicBoolean(false);
        }

        private void requestShutdown() {
            exitRequested.set(true);
        }

        public boolean shutdownRequested() {
            return exitRequested.get();
        }
    }

    @FunctionalInterface
    public interface PeriodicTask {
        void runTask(ShutdownSignal signal);
    }
}
