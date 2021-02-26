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

import java.io.InputStream;
import ldprotest.util.Result;
import ldprotest.util.TcpPort;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.error.YAMLException;

public class ConfigFile {

    private ConfigFile() {
        /* do not construct */
    }

    public static Result<String, AppConfig.Builder> parse(AppConfig.Builder builder, InputStream file) {
        ConfigFileData data;
        Yaml yaml = new Yaml(new Constructor(ConfigFileData.class));

        try {
            data = yaml.load(file);
        } catch(YAMLException ex) {
            return Result.failure(ex.getMessage());
        }

        if(data.serverPort != null) {

            if(TcpPort.validate(data.serverPort)) {
                builder.setServerPort(data.serverPort, AppConfig.PRIORITY_CONFIG);
            } else {
                return Result.failure("Invalid TCP port number: " + data.serverPort);
            }
        }

        if(data.usingHttps != null) {
            builder.setUsingHttps(data.usingHttps, AppConfig.PRIORITY_CONFIG);
        }
        if(data.mongoConnect != null) {
            builder.setConfigFilePath(data.mongoConnect, AppConfig.PRIORITY_CONFIG);
        }

        if(data.userSessionConfig != null) {
            UserSessionConfig config = data.userSessionConfig;

            if(config.sessionExpiresSeconds != null) {
                builder.setSessionExpiresSeconds(config.sessionExpiresSeconds, AppConfig.PRIORITY_CONFIG);
            }
            if(config.tokenKeyRotateSeconds != null) {
                builder.setTokenKeyRotateSeconds(config.tokenKeyRotateSeconds, AppConfig.PRIORITY_CONFIG);
            }
            if(config.tokenExpiresSeconds != null) {
                builder.setTokenExpiresSeconds(config.tokenExpiresSeconds, AppConfig.PRIORITY_CONFIG);
            }
        }

        if(data.keyStoreConfig != null) {
            KeyStoreConfig config = data.keyStoreConfig;

            if(config.fsKeyStorePath != null) {
                builder.setFsKeyStorePath(config.fsKeyStorePath, AppConfig.PRIORITY_CONFIG);
            }
        }

        return Result.success(builder);
    }

    private static final class ConfigFileData {
        public Integer serverPort;
        public Boolean usingHttps;
        public String mongoConnect;
        public UserSessionConfig userSessionConfig;
        public KeyStoreConfig keyStoreConfig;
    }

    private static final class UserSessionConfig {
        public Integer sessionExpiresSeconds;
        public Integer tokenKeyRotateSeconds;
        public Integer tokenKeyDeletionSeconds;
        public Integer tokenExpiresSeconds;
    }

    private static final class KeyStoreConfig {
        public String fsKeyStorePath;
    }
}
