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

import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import ldprotest.serialization.JsonSerializable;
import ldprotest.server.auth.HttpVerbTypes;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.auth.UserAccount;
import ldprotest.server.auth.UserAccount.UserLookupErrorCode;
import ldprotest.server.auth.UserInfo;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.server.infra.JsonEndpoint;
import ldprotest.server.infra.JsonError;
import ldprotest.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class User {

    private static final String PATH = "/api/user/*";
    private final static Logger LOGGER = LoggerFactory.getLogger(User.class);

    private final static EnumSet<UserRole> CAN_VIEW_OTHER_USER = EnumSet.of(
        UserRole.ADMIN, UserRole.MODERATOR
    );

    private static final Map <UserRole, Integer> VIEW_PERM_HEIRARCHY = Map.of(
        UserRole.USER, 0,
        UserRole.PLANNER, 0,
        UserRole.MODERATOR, 1,
        UserRole.ADMIN, 2
    );

    private User() {
        /* do not construct */
    }

    public static void register() {

        SecurityFilter.add(
            PATH,
            SecConfig.builder()
                .add(UserRole.USER, HttpVerbTypes.GET, HttpVerbTypes.DELETE)
                .add(UserRole.ADMIN, HttpVerbTypes.GET, HttpVerbTypes.DELETE)
                .add(UserRole.MODERATOR, HttpVerbTypes.GET, HttpVerbTypes.DELETE)
                .add(UserRole.PLANNER, HttpVerbTypes.GET, HttpVerbTypes.DELETE)
                .setAuthEndpoint()
                .build()
        );

        JsonEndpoint.get(PATH, (request, response) -> {

            String[] splats = request.splat();

            if(splats.length != 1) {
                LOGGER.error("Unexpected number of splat params: {}", splats.length);
                return JsonEndpoint.responseFromError(JsonError.internalError(), response);
            }

            String uname = splats[0];

            if(uname == null) {
                LOGGER.error("Unexpected null uname splat param: {}", request.pathInfo());
                return JsonEndpoint.responseFromError(JsonError.internalError(), response);
            }

            Optional<UserSessionInfo> optSesssionInfo = SecurityFilter.bearerSessionInfo(request);

            if(optSesssionInfo.isEmpty()) {
                LOGGER.error("Unexpected empty user session info");
                return JsonEndpoint.responseFromError(JsonError.internalError(), response);
            }

            UserSessionInfo sessionInfo = optSesssionInfo.get();
            boolean selfLookup = sessionInfo.username.equals(uname);

            if(!(CAN_VIEW_OTHER_USER.contains(sessionInfo.role) || selfLookup)) {
                LOGGER.warn(
                    "User {} with role {} attempted to access user info of {}",
                    sessionInfo.username,
                    sessionInfo.role,
                    uname
                );
                return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
            }

            Result<UserLookupErrorCode, UserInfo> lookupResult = UserAccount.lookupByUsername(uname);

            if(lookupResult.isFailure()) {
                if(lookupResult.failureReason().equals(UserLookupErrorCode.INVALID_USER)) {
                    return JsonEndpoint.responseFromError(JsonError.noSuchResource(), response);
                } else {
                    return JsonEndpoint.responseFromError(JsonError.internalError(), response);
                }
            }

            UserInfo userInfo = lookupResult.result();

            boolean viewHeirarchyOkay = (
                VIEW_PERM_HEIRARCHY.get(sessionInfo.role) > VIEW_PERM_HEIRARCHY.get(userInfo.userRole)
            );

            if(!viewHeirarchyOkay && !selfLookup) {
                LOGGER.warn("User {} attempted to view higher privillege user {}", sessionInfo.username, uname);
                return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
            }

            return new UserInfoResponseBody(
                userInfo.publicUsername,
                userInfo.email,
                userInfo.userRole
            );
        });
    }

    private static class UserInfoResponseBody implements JsonSerializable {
        public String username;
        public String email;
        public String role;

        public UserInfoResponseBody(String username, String email, UserRole role) {
            this.username = username;
            this.email = email;
            this.role = role.name();
        }
    }
}
