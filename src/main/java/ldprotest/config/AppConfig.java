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

import java.util.Optional;

public class AppConfig {

    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_CONFIG = 1;
    public static final int PRIORITY_OVERRIDE = 2;

    public final int tokenKeyDeletionSeconds;
    public final int tokenKeyRotateSeconds;
    public final int sessionExpiresSeconds;
    public final String mongoConnect;
    public final String configFilePath;
    public final String mapApiToken;
    public final boolean helpRequested;
    public final boolean usingHttps;

    private AppConfig(
        int tokenKeyDeletionSeconds,
        int tokenRefreshSeconds,
        int sessionExpiresSeconds,
        String mongoConnect,
        String configFilePath,
        String mapApiToken,
        boolean helpRequested,
        boolean usingHttps
    ) {
        this.tokenKeyDeletionSeconds = tokenKeyDeletionSeconds;
        this.tokenKeyRotateSeconds = tokenRefreshSeconds;
        this.sessionExpiresSeconds = sessionExpiresSeconds;
        this.mongoConnect = mongoConnect;
        this.configFilePath = configFilePath;
        this.mapApiToken = mapApiToken;
        this.helpRequested = helpRequested;
        this.usingHttps = usingHttps;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        public final BuilderField<Integer> tokenKeyDeletionSeconds;
        public final BuilderField<Integer> tokenKeyRotateSeconds;
        public final BuilderField<Integer> sessionExpiresSeconds;
        public final BuilderField<String> mongoConnect;
        public final BuilderField<String> configFilePath;
        public final BuilderField<String> mapApiToken;
        public final BuilderField<Boolean> helpRequested;
        public final BuilderField<Boolean> usingHttps;

        private Builder() {
            tokenKeyDeletionSeconds = new BuilderField<>();
            tokenKeyRotateSeconds =  new BuilderField<>();
            sessionExpiresSeconds = new BuilderField<>();
            mongoConnect = new BuilderField<>();
            configFilePath = new BuilderField<>();
            mapApiToken = new BuilderField<>();
            helpRequested = new BuilderField<>();
            usingHttps = new BuilderField<>();
        }

        public Builder setTokenKeyDeletionSeconds(int val, int priority) {
            tokenKeyDeletionSeconds.set(val, priority);
            return this;
        }

        public Builder setTokenKeyRotateSeconds(int val, int priority) {
            tokenKeyRotateSeconds.set(val, priority);
            return this;
        }

        public Builder setSessionExpiresSeconds(int  val, int priority) {
            sessionExpiresSeconds.set(val, priority);
            return this;
        }

        public Builder setMongoConnect(String val, int priority) {
            mongoConnect.set(val, priority);
            return this;
        }

        public Builder setConfigFilePath(String path, int priority) {
            configFilePath.set(path, priority);
            return this;
        }

        public Builder setMapApiToken(String token, int priority) {
            mapApiToken.set(token, priority);
            return this;
        }

        public Builder setHelpRequested(boolean val, int priority) {
            helpRequested.set(val, priority);
            return this;
        }

        public Builder setUsingHttps(boolean val, int priority) {
            usingHttps.set(val, priority);
            return this;
        }

        public AppConfig build() {
            return new AppConfig(
                tokenKeyDeletionSeconds.get(),
                tokenKeyRotateSeconds.get(),
                sessionExpiresSeconds.get(),
                mongoConnect.get(),
                configFilePath.get(),
                mapApiToken.get(),
                helpRequested.get(),
                usingHttps.get()
            );
        }
    }

    public static final class BuilderField<T> {
        private Optional<T> val;
        private int priority;

        private BuilderField() {
            val = Optional.empty();
            priority = -1;
        }

        private void set(T val, int priority) {
            if(priority > this.priority) {
                this.val = Optional.of(val);
                this.priority = priority;
            }
        }

        public T get() {
            return val.get();
        }
    }
}
