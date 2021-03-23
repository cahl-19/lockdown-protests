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

public final class DefaultConfig {

    private static final String DEFAULT_MONGO_CONNECT =
        "mongodb://ldprotest:ldprotest@localhost:27017/?serverSelectionTimeoutMS=3000";

    static private final int DEFAULT_HSTS_MAX_AGE = 0;
    static private final long DEFAULT_HTTP_CACHE_MAX_AGE = 60;
    static private final int DEFAULT_SERVER_PORT = 4567;
    static private final int DEFAULT_SESSION_EXPIRES_SECONDS = 3600 * 24 * 7;
    static private final int DEFAULT_TOKEN_KEY_ROTATE_SECONDS =  3600 * 24 * 30;
    static private final int DEFAULT_TOKEN_KEY_DELETE_SECONDS = DEFAULT_TOKEN_KEY_ROTATE_SECONDS * 2;
    static private final int DEFAULT_TOKEN_EXPIRES_SECONDS = 15 * 60;

    private DefaultConfig() {
        /* do not construct */
    }

    public static AppConfig.Builder defaultBuilder() {

        AppConfig.Builder builder = AppConfig.builder();

        builder.setStyleOptions(StyleCustomizationOptions.DEFAULT, AppConfig.PRIORITY_DEFAULT);
        builder.setHstsMaxAge(DEFAULT_HSTS_MAX_AGE, AppConfig.PRIORITY_DEFAULT);
        builder.setRotatingTokenConfig(RotatingTokenMapboxConfig.UNSET_INSTANCE, AppConfig.PRIORITY_DEFAULT);
        builder.setTemporaryTokenConfig(TemporaryTokenMapboxConfig.UNSET_INSTANCE, AppConfig.PRIORITY_DEFAULT);
        builder.setLogbackPath("", AppConfig.PRIORITY_DEFAULT);
        builder.setHttpCacheMaxAge(DEFAULT_HTTP_CACHE_MAX_AGE, AppConfig.PRIORITY_DEFAULT);
        builder.setServerPort(DEFAULT_SERVER_PORT, AppConfig.PRIORITY_DEFAULT);
        builder.setFsKeyStorePath("", AppConfig.PRIORITY_DEFAULT);
        builder.setTokenExpiresSeconds(DEFAULT_TOKEN_EXPIRES_SECONDS, AppConfig.PRIORITY_DEFAULT);
        builder.setTokenKeyDeletionSeconds(DEFAULT_TOKEN_KEY_DELETE_SECONDS, AppConfig.PRIORITY_DEFAULT);
        builder.setTokenKeyRotateSeconds(DEFAULT_TOKEN_KEY_ROTATE_SECONDS, AppConfig.PRIORITY_DEFAULT);
        builder.setSessionExpiresSeconds(DEFAULT_SESSION_EXPIRES_SECONDS, AppConfig.PRIORITY_DEFAULT);
        builder.setConfigFilePath("", AppConfig.PRIORITY_DEFAULT);
        builder.setUsingHttps(true, AppConfig.PRIORITY_DEFAULT);
        builder.setStaticMapApiToken("", AppConfig.PRIORITY_DEFAULT);
        builder.setHelpRequested(false, AppConfig.PRIORITY_DEFAULT);

        builder.setMongoConnect(DEFAULT_MONGO_CONNECT, AppConfig.PRIORITY_DEFAULT);

        return builder;
    }
}
