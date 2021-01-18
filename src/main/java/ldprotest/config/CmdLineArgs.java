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
package ldprotest.config;

import java.util.concurrent.Callable;
import ldprotest.util.Result;
import picocli.CommandLine;
import picocli.CommandLine.Option;

public class CmdLineArgs {

    public final String mapApiToken;
    public final boolean helpRequested;

    private CmdLineArgs(Args args) {
        this.mapApiToken = args.mapApiToken;
        this.helpRequested = args.helpRequested;
    }

    public static Result<Integer, CmdLineArgs> parse(String... cmdLine) {
        Args args = new Args();
        int exitCode = new CommandLine(args).execute(cmdLine);

        if(exitCode != 0) {
            return Result.failure(exitCode);
        } else {
            return Result.success(new CmdLineArgs(args));
        }
    }

    private static class Args implements Callable<Integer> {

        @Option(names = {"--map-api-token"}, description = "mapbox API token")
        private String mapApiToken = "";

        @Option(names = { "-h", "--help" }, usageHelp = true, description = "display help text")
        private boolean helpRequested = false;

        @Override
        public Integer call() throws Exception {
            return 0;
        }
    }
}
