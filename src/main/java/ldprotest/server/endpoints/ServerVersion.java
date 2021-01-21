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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;
import ldprotest.serialization.JsonSerializable;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.infra.JsonEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ServerVersion {

    private final static Logger LOGGER = LoggerFactory.getLogger(ServerVersion.class);

    private static final String PATH = "/api/version";
    private static final String VERSION_RESOURCE_PATH = "info/version.txt";

    private ServerVersion() {
        /* do not construct */
    }

    public static void register() {
        JsonVersion version = readVersionString();
        SecurityFilter.add(PATH, SecConfig.ANONYMOUS_GET);
        JsonEndpoint.get(PATH, (request, response) -> version);
    }

    private static JsonVersion readVersionString() {
        InputStream is = ServerVersion.class.getClassLoader().getResourceAsStream(VERSION_RESOURCE_PATH);

        try {
            return new JsonVersion(new String(is.readAllBytes(), StandardCharsets.US_ASCII).strip());
        } catch (IOException ex) {
            LOGGER.error("Unable to read version resource", ex);
            return new JsonVersion("0.0");
        }
    }

    private static class JsonVersion implements JsonSerializable {
        public final String version;

        public JsonVersion(String version) {
            this.version = version;
        }
    }
}
