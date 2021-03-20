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
import java.util.List;
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

        if(data.httpCacheMaxAge != null) {
            if(data.httpCacheMaxAge >= 0) {
                builder.setHttpCacheMaxAge(data.httpCacheMaxAge, AppConfig.PRIORITY_CONFIG);
            } else {
                return Result.failure("Invalid Age Value: " + data.httpCacheMaxAge);
            }
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
            builder.setMongoConnect(data.mongoConnect, AppConfig.PRIORITY_CONFIG);
        }

        if(data.logbackPath != null) {
            builder.setLogbackPath(data.logbackPath, AppConfig.PRIORITY_CONFIG);
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

        if(data.mapboxConfig != null) {
            MapboxConfig config = data.mapboxConfig;

            if(config.staticConfig != null) {
                StaticMapboxConfig staticConfig = config.staticConfig;

                if(staticConfig.apiToken != null) {
                    builder.setStaticMapApiToken(staticConfig.apiToken, AppConfig.PRIORITY_CONFIG);
                }
            }
            if(config.temporaryTokenConfig != null) {
                ConfigFileTemporaryTokenConfig tempConf = config.temporaryTokenConfig;

                TemporaryTokenMapboxConfig configObj = new TemporaryTokenMapboxConfig(
                        defaultIfNull(tempConf.username, ""),
                        defaultIfNull(tempConf.accessToken, ""),
                        defaultIfNull(tempConf.expiresSeconds, TemporaryTokenMapboxConfig.DEFAULT_EXPIRES),
                        defaultIfNull(tempConf.renewSeconds, TemporaryTokenMapboxConfig.DEFAULT_RENEW),
                        defaultIfNull(
                            tempConf.clientTokenRefreshSeconds, TemporaryTokenMapboxConfig.DEFAULT_CLIENT_REFRESH
                        )
                );

                if(configObj.expiresSeconds > 3600) {
                    return Result.failure("Mapbox temporary token expires cannot be greater than 1 hour");
                }

                builder.setTemporaryTokenConfig(configObj, AppConfig.PRIORITY_CONFIG);
            }

            if(config.rotatingTokenConfig != null) {
                ConfigFileRotatingTokeConfig rotConf = config.rotatingTokenConfig;

                RotatingTokenMapboxConfig configObj = new RotatingTokenMapboxConfig(
                    defaultIfNull(rotConf.username, ""),
                    defaultIfNull(rotConf.accessToken, ""),
                    defaultIfNull(rotConf.expiresSeconds, RotatingTokenMapboxConfig.DEFAULT_EXPIRES),
                    defaultIfNull(rotConf.renewSeconds, RotatingTokenMapboxConfig.DEFAULT_RENEW),
                    defaultIfNull(
                        rotConf.clientTokenRefreshSeconds, RotatingTokenMapboxConfig.DEFAULT_CLIENT_REFRESH
                    ),
                    defaultIfNull(rotConf.allowedUrls, List.of()),
                    defaultIfNull(rotConf.tokenNamePattern, ""),
                        defaultIfNull(rotConf.serverPollSeconds, RotatingTokenMapboxConfig.DEFAULT_SERVER_POLL)
                );
                builder.setRotatingTokenConfig(configObj, AppConfig.PRIORITY_CONFIG);
            }

        }

        return Result.success(builder);
    }

    private static <T> T defaultIfNull(T field, T def) {
        return field != null ? field : def;
    }

    private static final class ConfigFileData {
        public Integer serverPort;
        public Boolean usingHttps;
        public Long httpCacheMaxAge;
        public String mongoConnect;
        public UserSessionConfig userSessionConfig;
        public KeyStoreConfig keyStoreConfig;
        public String logbackPath;
        public MapboxConfig mapboxConfig;
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

    private static final class MapboxConfig {
        public StaticMapboxConfig staticConfig;
        public ConfigFileTemporaryTokenConfig temporaryTokenConfig;
        public ConfigFileRotatingTokeConfig rotatingTokenConfig;
    }

    private static final class StaticMapboxConfig {
        public String apiToken;
    }

    private static final class ConfigFileTemporaryTokenConfig {
        public String username;
        public String accessToken;
        public Integer expiresSeconds;
        public Integer renewSeconds;
        public Integer clientTokenRefreshSeconds;
    }

    private static final class ConfigFileRotatingTokeConfig {
        public String username;
        public String accessToken;
        public int expiresSeconds;
        public int renewSeconds;
        public int clientTokenRefreshSeconds;
        public List<String> allowedUrls;
        public String tokenNamePattern;
        public int serverPollSeconds;
    }
}
