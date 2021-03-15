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

import ldprotest.main.Main;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.infra.JsonEndpoint;

public final class MapApiToken {

    private static final String PATH = "/api/map-api-token";

    private MapApiToken() {
        /* do not construct */
    }

    public static void register() {

        SecurityFilter.add(PATH, SecConfig.ANONYMOUS_GET);

        JsonEndpoint.get(PATH, (request, response) -> {
            return new JsonDoc(Main.args().mapApiToken);
        });
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
