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
import ldprotest.util.TcpPort;
import picocli.CommandLine;
import picocli.CommandLine.Option;
import picocli.CommandLine.TypeConversionException;

public class CmdLineArgs {

    private CmdLineArgs() {
        /* do not construct */
    }

    public static Result<Integer, AppConfig.Builder> parse(AppConfig.Builder builder, String... cmdLine) {
        Args args = new Args(builder);
        CommandLine commandLine = new CommandLine(args);
        int exitCode = commandLine.execute(cmdLine);

        if(exitCode != 0) {
            return Result.failure(exitCode);
        } else {
            return Result.success(builder);
        }
    }
    private static class Args implements Callable<Integer>{

        private final AppConfig.Builder builder;

        public Args(AppConfig.Builder builder) {
            this.builder = builder;
        }

        @Option(names = {"--mongo-connect"}, description = "Override the Mongo connect string")
        void mongoConnectString(String val) {
            builder.setMongoConnect(val, AppConfig.PRIORITY_OVERRIDE);
        }

        @Option(names = {"--static-map-api-token"}, description = "mapbox API token to use for loading map tiles")
        void staticMapApiToken(String token) {
            builder.setStaticMapApiToken(token, AppConfig.PRIORITY_OVERRIDE);
        }

        @Option(names = {"--no-https"}, description = "for development purposes only: assume that https is disabled")
        void usingHttps (boolean value) {
            builder.setUsingHttps(!value, AppConfig.PRIORITY_OVERRIDE);
        }

        @Option(names = { "-h", "--help" }, usageHelp = true, description = "display help text")
        void helpRequested(boolean val) {
            builder.setHelpRequested(val, AppConfig.PRIORITY_OVERRIDE);
        }

        @Option(names = { "-f", "--config-file"}, description = "Path to configuration file. No config file if not set")
        void configFilePath(String path) {
            builder.setConfigFilePath(path, AppConfig.PRIORITY_OVERRIDE);
        }

        @Option(names = {"--fs-key-store-path"}, description = "directory to store JWT signing keys")
        void fsKeyStorePath(String path) {
            builder.setFsKeyStorePath(path, AppConfig.PRIORITY_OVERRIDE);
        }

        @Option(names = {"--server-port"}, description = "TCP port which the HTTP server listens on")
        void serverPort(int port) {
            if(!TcpPort.validate(port)) {
                throw new TypeConversionException("Invalid TCP port number");
            }
            builder.setServerPort(port, AppConfig.PRIORITY_OVERRIDE);
        }

        @Option(names = {"--logback-path"}, description = "Path to logback.xml file to configure logging")
        void logbackPath(String path) {
            builder.setLogbackPath(path, AppConfig.PRIORITY_OVERRIDE);
        }

        @Option(names = {"--disable-public-login"}, description = "Disables login side bar and buttons on main page")
        void disablePublicLogin(boolean val) {
            builder.setDisablePublicLogin(val, AppConfig.PRIORITY_OVERRIDE);
        }

        @Option(names = {"--enable-geo-ip"}, description = "Enables geo ip lookups")
        void enableGeoIp(boolean val) {
            builder.setDisableGeoIpLookup(!val, AppConfig.PRIORITY_OVERRIDE);
        }

        @Override
        public Integer call() throws Exception {
            return 0;
        }
    }
}
