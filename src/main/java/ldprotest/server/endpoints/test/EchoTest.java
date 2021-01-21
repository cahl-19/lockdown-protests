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
package ldprotest.server.endpoints.test;

import java.util.Optional;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.server.auth.HttpVerbTypes;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.auth.UserRole;
import ldprotest.server.infra.JsonEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EchoTest {
    private final static Logger LOGGER = LoggerFactory.getLogger(EchoTest.class);

    private static final String PATH = "/api/test/echo";
    private static final String VERSION_RESOURCE_PATH = "info/version.txt";

    private EchoTest() {
        /* do not construct */
    }

    public static void register() {

        SecurityFilter.add(
            PATH,
            SecConfig.builder()
                .add(UserRole.USER, HttpVerbTypes.POST)
                .add(UserRole.MODERATOR, HttpVerbTypes.POST)
                .add(UserRole.PLANNER, HttpVerbTypes.POST)
                .add(UserRole.UNAUTHENTICATED, HttpVerbTypes.POST)
                .build()
        );

        JsonEndpoint.post(PATH, EchoClass.class, (echoRequest, request, response) -> {
            LOGGER.info("Echo request on string: {}", echoRequest.message.orElse(""));
            return echoRequest;
        });
    }

    private static class EchoClass implements JsonSerializable {
        public final Optional<String> message;

        @ReflectiveConstructor
        private EchoClass() {
            this.message = Optional.empty();
        }

        public EchoClass(String message) {
            this.message = Optional.of(message);
        }
    }
}
