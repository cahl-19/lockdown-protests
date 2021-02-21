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
import ldprotest.main.Main;
import ldprotest.serialization.JsonSerializable;
import ldprotest.server.auth.HttpVerbTypes;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.server.infra.JsonEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WhoAmI {

    private static final String PATH = "/api/whoami";
    private final static Logger LOGGER = LoggerFactory.getLogger(WhoAmI.class);

    private WhoAmI() {
        /* do not construct */
    }

    public static void register() {

        boolean usingHttps = Main.args().usingHttps;

        if(!usingHttps) {
            LOGGER.warn("Using HTTPS=false. Login cookies will not have the secure flag.");
        }

        SecurityFilter.add(
            PATH,
            SecConfig.builder()
                .add(UserRole.USER, HttpVerbTypes.GET)
                .add(UserRole.UNAUTHENTICATED, HttpVerbTypes.GET)
                .add(UserRole.ADMIN, HttpVerbTypes.GET)
                .add(UserRole.MODERATOR, HttpVerbTypes.GET)
                .add(UserRole.PLANNER, HttpVerbTypes.GET)
                .build()
        );

        JsonEndpoint.get(PATH, (request, response) -> {

            Optional<UserSessionInfo> optHttpInfo = SecurityFilter.httpSessionInfo(request);
            Optional<UserSessionInfo> optBearerInfo = SecurityFilter.bearerSessionInfo(request);

            return new UserInfo(
                optHttpInfo.isPresent() ? Optional.of(optHttpInfo.get().username) : Optional.empty(),
                SecurityFilter.userRoleAttr(request),
                optBearerInfo.isPresent()
            );
        });
    }

    private static final class UserInfo implements JsonSerializable {

        public final Optional<String> username;
        public final UserRole role;

        public final boolean apiTokenValid;

        public UserInfo(Optional<String> username, UserRole role, boolean apiTokenValid) {
            this.username = username;
            this.role = role;
            this.apiTokenValid = apiTokenValid;
        }
    }
}
