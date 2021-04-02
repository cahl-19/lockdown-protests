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

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import ldprotest.business.mapbox.MapboxTokenApi;
import ldprotest.business.mapbox.MapboxTokenApi.ApiFailure;
import ldprotest.business.mapbox.MapboxTokenApi.TemporaryTokenResponse;
import ldprotest.business.mapbox.MapboxTokenRotator;
import ldprotest.config.RotatingTokenMapboxConfig;
import ldprotest.config.TemporaryTokenMapboxConfig;
import ldprotest.geo.Coordinate;
import ldprotest.geo.geoip.GeoIpLookup;
import ldprotest.main.Main;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.infra.JsonEndpoint;
import ldprotest.server.infra.http.ClientIp;
import ldprotest.tasks.PeriodicTaskManager;
import ldprotest.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MapConfig {

    private final static Logger LOGGER = LoggerFactory.getLogger(MapConfig.class);

    private static final String TOKEN_PATH = "/api/map-api-token";
    private static final String INITIAL_CONFIG_PATH = "/api/map-config";

    private MapConfig() {
        /* do not construct */
    }

    public static void register() {

        boolean geoIpEnabled = !Main.args().disableGeoIpLookup;

        SecurityFilter.add(TOKEN_PATH, SecConfig.ANONYMOUS_GET);
        SecurityFilter.add(INITIAL_CONFIG_PATH, SecConfig.ANONYMOUS_GET);

        Supplier<String> tokenSupplier = createTokenSupplier();

        JsonEndpoint.get(TOKEN_PATH, (request, response) -> {
            return new TokenDoc(tokenSupplier.get());
        });
        JsonEndpoint.get(INITIAL_CONFIG_PATH, (request, response) -> {
            if(geoIpEnabled) {
                Result<GeoIpLookup.GeoIpLookupError, Coordinate> result = GeoIpLookup.lookup(ClientIp.get(request));
                if(result.isSuccess()) {
                    return new ConfigDoc(tokenSupplier.get(), new Coordinate(
                        result.result().latitude, result.result().longitude)
                    );
                }

            }

            return new ConfigDoc(tokenSupplier.get());
        });
    }

    private static final Supplier<String> createTokenSupplier() {
        String staticMapApiToken = Main.args().staticMapApiToken;
        TemporaryTokenMapboxConfig temporaryTokenConfig = Main.args().temporaryTokenConfig;
        RotatingTokenMapboxConfig rotConfig = Main.args().rotatingTokenConfig;

        if(rotConfig.isFullyDefined()) {
            MapboxTokenRotator rotator = new MapboxTokenRotator(rotConfig, staticMapApiToken);
            rotator.register();
            return () -> rotator.get();

        } else if(temporaryTokenConfig.isFullyDefined()) {
            TemporaryTokenHolder holder = new TemporaryTokenHolder(temporaryTokenConfig, staticMapApiToken);
            holder.register();
            return () -> holder.get();
        } else {
            return () -> Main.args().staticMapApiToken;
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

    private static class TokenDoc implements JsonSerializable {
        public final String token;

        @ReflectiveConstructor
        private TokenDoc() {
            token = "";
        }

        public TokenDoc(String token) {
            this.token = token;
        }
    }

    private static class ConfigDoc implements JsonSerializable {

        public final String token;
        public final Optional<Coordinate> geoIpLocation;

        @ReflectiveConstructor
        private ConfigDoc() {
            token = "";
            geoIpLocation = null;
        }

        public ConfigDoc(String token, Coordinate geoIpLocation) {
            this.token = token;
            this.geoIpLocation = Optional.of(geoIpLocation);
        }

        public ConfigDoc(String token) {
            this.token = token;
            this.geoIpLocation = Optional.empty();
        }
    }
}
