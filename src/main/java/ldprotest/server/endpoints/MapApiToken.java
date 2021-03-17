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
package ldprotest.server.endpoints;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import ldprotest.business.mapbox.MapboxTokenApi;
import ldprotest.business.mapbox.MapboxTokenApi.ApiFailure;
import ldprotest.business.mapbox.MapboxTokenApi.TemporaryTokenResponse;
import ldprotest.config.TemporaryTokenMapboxConfig;
import ldprotest.main.Main;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.infra.JsonEndpoint;
import ldprotest.tasks.PeriodicTaskManager;
import ldprotest.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MapApiToken {

    private final static Logger LOGGER = LoggerFactory.getLogger(MapApiToken.class);

    private static final String PATH = "/api/map-api-token";

    private MapApiToken() {
        /* do not construct */
    }

    public static void register() {

        SecurityFilter.add(PATH, SecConfig.ANONYMOUS_GET);

        String staticMapApiToken = Main.args().staticMapApiToken;
        TemporaryTokenMapboxConfig temporaryTokenConfig = Main.args().temporaryTokenConfig;

        if(temporaryTokenConfig.isFullyDefined()) {

            TemporaryTokenHolder holder = new TemporaryTokenHolder(temporaryTokenConfig, staticMapApiToken);
            holder.register();

            JsonEndpoint.get(PATH, (request, response) -> {
                return new JsonDoc(holder.get());
            });
        } else {

            JsonEndpoint.get(PATH, (request, response) -> {
                return new JsonDoc(Main.args().staticMapApiToken);
            });
        }
    }

    private static class TemporaryTokenHolder implements PeriodicTaskManager.PeriodicTask {

        private final String username;
        private final String accessToken;
        private final int expires;
        private final int renewal;

        private final String staticToken;

        private final AtomicReference<String> token;

        public TemporaryTokenHolder(TemporaryTokenMapboxConfig temporaryTokenConfig, String staticToken) {

            this.username = temporaryTokenConfig.username;
            this.accessToken = temporaryTokenConfig.accessToken;
            this.expires = temporaryTokenConfig.expiresSeconds;
            this.renewal = temporaryTokenConfig.renewSeconds;

            this.staticToken = staticToken;
            this.token = new AtomicReference<>(staticToken);
        }

        @Override
        public void runTask(PeriodicTaskManager.ShutdownSignal signal) {
            refresh();
        }

        public void register() {
            PeriodicTaskManager.registerTask(0, renewal, TimeUnit.SECONDS, true, this);
        }

        private void refresh() {
            Result<ApiFailure, TemporaryTokenResponse> result = MapboxTokenApi.createTemporaryToken(
                username, accessToken, expires
            );

            if(result.isFailure()) {
                LOGGER.warn("Failed to fetch new token. Falling back to static config");
                token.set(staticToken);
            } else {
                token.set(result.result().token);
            }
        }

        public String get() {
            return token.get();
        }
    }

    private static class JsonDoc implements JsonSerializable {
        public final String token;

        @ReflectiveConstructor
        private JsonDoc() {
            token = "";
        }

        public JsonDoc(String token) {
            this.token = token;
        }
    }
}
