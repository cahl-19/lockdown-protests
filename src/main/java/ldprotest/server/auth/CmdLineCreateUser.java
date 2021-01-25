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
package ldprotest.server.auth;

import com.mongodb.MongoException;
import java.io.IOException;
import java.util.concurrent.Callable;
import ldprotest.db.MainDatabase;
import ldprotest.db.SetupDatabase;
import ldprotest.main.AppLogging;
import static ldprotest.main.AppLogging.ResourceType.ON_CLASSPATH;
import ldprotest.util.ErrorCode;
import ldprotest.util.Result;
import picocli.CommandLine;


public final class CmdLineCreateUser {

    private static final String LOGBACK_CONFIG = "/config/logback-off.xml";

    private CmdLineCreateUser() {
        /* do not construct */
    }

    public static void main(String args[]) {
        Result<Integer, Args> parseResult = parseArgs(args);

        if(parseResult.isFailure()) {
            System.exit(parseResult.failureReason());
        }

        Args options = parseResult.result();

        if(options.helpRequested) {
            System.exit(0);
        }

        try {
            AppLogging.setLogbackConfig(LOGBACK_CONFIG, ON_CLASSPATH);
        }
        catch(IOException e) {
            System.err.println("Error setting logging configuration:" + e);
            System.exit(-1);
        }

        MainDatabase.connect("mongodb://ldprotest:ldprotest@localhost:27017/?serverSelectionTimeoutMS=3000");

        ErrorCode<MongoException> e = MainDatabase.testConnection();

        if(e.failed()) {
            System.err.println("No database connection at startup:" + e);
            System.exit(-1);
        }

        SetupDatabase.setup(MainDatabase.database());

        Result<UserAccount.UserCreationError, UserInfo> createUserResult = UserAccount.create(
            options.username, options.emailAddress, options.role, options.password
        );

        if(createUserResult.isFailure()) {
            System.err.println("Failed to create user: " + createUserResult.failureReason());
            System.exit(-1);
        }

        System.exit(0);
    }

    public static Result<Integer, Args> parseArgs(String... cmdLine) {
        Args args = new Args();
        int exitCode = new CommandLine(args).execute(cmdLine);

        if(exitCode != 0) {
            return Result.failure(exitCode);
        } else {
            return Result.success(args);
        }
    }

    private static class Args implements Callable<Integer> {

        @CommandLine.Parameters(index="0", description = "user's name")
        public String username = null;

        @CommandLine.Parameters(index="1", description = "user's email address")
        public String emailAddress = null;

        @CommandLine.Parameters(index="2", description = "User's password")
        public String password = null;

        @CommandLine.Option(
            names={"-r", "--role"},
            description="user's role (default is ADMIN). Legal values: ${COMPLETION-CANDIDATES}"
        )
        public UserRole role = UserRole.ADMIN;

        @CommandLine.Option(names = { "-h", "--help" }, usageHelp = true, description = "display help text")
        private boolean helpRequested = false;

        @Override
        public Integer call() throws Exception {
            return 0;
        }
    }
}

