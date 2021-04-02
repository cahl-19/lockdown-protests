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

import java.util.List;
import java.util.Optional;

public class AppConfig {

    public static final int PRIORITY_DEFAULT = 0;
    public static final int PRIORITY_CONFIG = 1;
    public static final int PRIORITY_OVERRIDE = 2;

    public final boolean usingReverseProxy;
    public final List<String> attributions;
    public final boolean disableGeoIpLookup;
    public final boolean disablePublicLogin;
    public final StyleCustomizationOptions styleOptions;
    public final int hstsMaxAge;
    public final RotatingTokenMapboxConfig rotatingTokenConfig;
    public final TemporaryTokenMapboxConfig temporaryTokenConfig;
    public final String logbackPath;
    public final long httpCacheMaxAge;
    public final int serverPort;
    public final String  fsKeyStorePath;
    public final int tokenExpiresSeconds;
    public final int tokenKeyDeletionSeconds;
    public final int tokenKeyRotateSeconds;
    public final int sessionExpiresSeconds;
    public final String mongoConnect;
    public final String configFilePath;
    public final String staticMapApiToken;
    public final boolean helpRequested;
    public final boolean usingHttps;

    private AppConfig(
        boolean usingReverseProxy,
        List<String> attributions,
        boolean disableGeoIpLookup,
        boolean disablePublicLogin,
        StyleCustomizationOptions styleOptions,
        int hstsMaxAge,
        RotatingTokenMapboxConfig rotatingTokenConfig,
        TemporaryTokenMapboxConfig temporaryTokenConfig,
        String logbackPath,
        long httpCacheMaxAge,
        int serverPort,
        String fsKeyStorePath,
        int tokenExpirySeconds,
        int tokenKeyDeletionSeconds,
        int tokenRefreshSeconds,
        int sessionExpiresSeconds,
        String mongoConnect,
        String configFilePath,
        String mapApiToken,
        boolean helpRequested,
        boolean usingHttps
    ) {
        this.usingReverseProxy = usingReverseProxy;
        this.attributions = List.copyOf(attributions);
        this.disableGeoIpLookup = disableGeoIpLookup;
        this.disablePublicLogin = disablePublicLogin;
        this.styleOptions = styleOptions;
        this.hstsMaxAge = hstsMaxAge;
        this.rotatingTokenConfig = rotatingTokenConfig;
        this.temporaryTokenConfig  = temporaryTokenConfig;
        this.logbackPath = logbackPath;
        this.httpCacheMaxAge = httpCacheMaxAge;
        this.serverPort = serverPort;
        this.fsKeyStorePath = fsKeyStorePath;
        this.tokenExpiresSeconds = tokenExpirySeconds;
        this.tokenKeyDeletionSeconds = tokenKeyDeletionSeconds;
        this.tokenKeyRotateSeconds = tokenRefreshSeconds;
        this.sessionExpiresSeconds = sessionExpiresSeconds;
        this.mongoConnect = mongoConnect;
        this.configFilePath = configFilePath;
        this.staticMapApiToken = mapApiToken;
        this.helpRequested = helpRequested;
        this.usingHttps = usingHttps;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        public final BuilderField<Boolean> usingReverseProxy;
        public final BuilderField<List<String>> attributions;
        public final BuilderField<Boolean> disableGeoIpLookup;
        public final BuilderField<Boolean> disablePublicLogin;
        public final BuilderField<StyleCustomizationOptions> styleOptions;
        public final BuilderField<Integer> hstsMaxAge;
        public final BuilderField<RotatingTokenMapboxConfig> rotatingTokenConfig;
        public final BuilderField<TemporaryTokenMapboxConfig> temporaryTokenConfig;
        public final BuilderField<String> logbackPath;
        public final BuilderField<Long> httpCacheMaxAge;
        public final BuilderField<Integer> serverPort;
        public final BuilderField<String> fsKeyStorePath;
        public final BuilderField<Integer> tokenExpiresSeconds;
        public final BuilderField<Integer> tokenKeyDeletionSeconds;
        public final BuilderField<Integer> tokenKeyRotateSeconds;
        public final BuilderField<Integer> sessionExpiresSeconds;
        public final BuilderField<String> mongoConnect;
        public final BuilderField<String> configFilePath;
        public final BuilderField<String> staticMapApiToken;
        public final BuilderField<Boolean> helpRequested;
        public final BuilderField<Boolean> usingHttps;

        private Builder() {
            usingReverseProxy = new BuilderField<>();
            attributions = new BuilderField<>();
            disableGeoIpLookup = new BuilderField<>();
            disablePublicLogin = new BuilderField<>();
            styleOptions = new BuilderField<>();
            hstsMaxAge = new BuilderField<>();
            rotatingTokenConfig = new BuilderField<>();
            temporaryTokenConfig = new BuilderField<>();
            logbackPath = new BuilderField<>();
            httpCacheMaxAge = new BuilderField<>();
            serverPort = new BuilderField<>();
            fsKeyStorePath = new BuilderField<>();
            tokenExpiresSeconds = new BuilderField<>();
            tokenKeyDeletionSeconds = new BuilderField<>();
            tokenKeyRotateSeconds =  new BuilderField<>();
            sessionExpiresSeconds = new BuilderField<>();
            mongoConnect = new BuilderField<>();
            configFilePath = new BuilderField<>();
            staticMapApiToken = new BuilderField<>();
            helpRequested = new BuilderField<>();
            usingHttps = new BuilderField<>();
        }

        public Builder setUsingReverseProxy(boolean val, int priority) {
            usingReverseProxy.set(val, priority);
            return this;
        }

        public Builder setAttributions(List<String> val, int priority) {
            attributions.set(List.copyOf(val), priority);
            return this;
        }

        public Builder setDisableGeoIpLookup(boolean val, int priority) {
            disableGeoIpLookup.set(val, priority);
            return this;
        }

        public Builder setDisablePublicLogin(boolean val, int priority) {
            disablePublicLogin.set(val, priority);
            return this;
        }

        public Builder setStyleOptions(StyleCustomizationOptions val, int priority) {
            styleOptions.set(val, priority);
            return this;
        }

        public Builder setHstsMaxAge(int val, int priority) {
            hstsMaxAge.set(val, priority);
            return this;
        }

        public Builder setRotatingTokenConfig(RotatingTokenMapboxConfig val, int priority) {
            rotatingTokenConfig.set(val, priority);
            return this;
        }

        public Builder setTemporaryTokenConfig(TemporaryTokenMapboxConfig val, int priority) {
            temporaryTokenConfig.set(val, priority);
            return this;
        }

        public Builder setLogbackPath(String path, int priority) {
             logbackPath.set(path, priority);
            return this;
        }

        public Builder setHttpCacheMaxAge(long val, int priority) {
            httpCacheMaxAge.set(val, priority);
            return this;
        }

        public Builder setServerPort(int val, int priority) {
            serverPort.set(val, priority);
            return this;
        }

        public Builder setFsKeyStorePath(String val, int priority) {
            fsKeyStorePath.set(val, priority);
            return this;
        }

        public Builder setTokenExpiresSeconds(int val, int priority) {
            tokenExpiresSeconds.set(val, priority);
            return this;
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

        public Builder setStaticMapApiToken(String token, int priority) {
            staticMapApiToken.set(token, priority);
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
                usingReverseProxy.get(),
                attributions.get(),
                disableGeoIpLookup.get(),
                disablePublicLogin.get(),
                styleOptions.get(),
                hstsMaxAge.get(),
                rotatingTokenConfig.get(),
                temporaryTokenConfig.get(),
                logbackPath.get(),
                httpCacheMaxAge.get(),
                serverPort.get(),
                fsKeyStorePath.get(),
                tokenExpiresSeconds.get(),
                tokenKeyDeletionSeconds.get(),
                tokenKeyRotateSeconds.get(),
                sessionExpiresSeconds.get(),
                mongoConnect.get(),
                configFilePath.get(),
                staticMapApiToken.get(),
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
