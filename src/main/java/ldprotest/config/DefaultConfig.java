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

    static private final int DEFAULT_SESSION_EXPIRES_SECONDS = 3600 * 24 * 7;
    static private final int DEFAULT_TOKEN_KEY_ROTATE_SESSIONS =  3600 * 24 * 30;

    private DefaultConfig() {
        /* do not construct */
    }

    public static AppConfig.Builder defaultBuilder() {

        AppConfig.Builder builder = AppConfig.builder();

        builder.setTokenKeyRotateSeconds(DEFAULT_TOKEN_KEY_ROTATE_SESSIONS, AppConfig.PRIORITY_DEFAULT);
        builder.setSessionExpiresSeconds(DEFAULT_SESSION_EXPIRES_SECONDS, AppConfig.PRIORITY_DEFAULT);
        builder.setConfigFilePath("", AppConfig.PRIORITY_DEFAULT);
        builder.setUsingHttps(true, AppConfig.PRIORITY_DEFAULT);
        builder.setMapApiToken("", AppConfig.PRIORITY_DEFAULT);
        builder.setHelpRequested(false, AppConfig.PRIORITY_DEFAULT);

        builder.setMongoConnect(DEFAULT_MONGO_CONNECT, AppConfig.PRIORITY_DEFAULT);

        return builder;
    }
}
