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
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.server.auth.HttpVerbTypes;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.server.auth.webtoken.UserTokenSubject;
import ldprotest.server.auth.webtoken.UserTokens;
import ldprotest.server.infra.JsonEndpoint;
import ldprotest.server.infra.JsonError;
import ldprotest.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TokenRefresh {

    public static final String LOGIN_COOKIE_NAME = "login-token";

    private static final String PATH = "/api/refresh-token";
    private final static Logger LOGGER = LoggerFactory.getLogger(TokenRefresh.class);

    private TokenRefresh() {
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
                .add(UserRole.USER, HttpVerbTypes.POST)
                .add(UserRole.UNAUTHENTICATED, HttpVerbTypes.POST)
                .add(UserRole.ADMIN, HttpVerbTypes.POST)
                .add(UserRole.MODERATOR, HttpVerbTypes.POST)
                .add(UserRole.PLANNER, HttpVerbTypes.POST)
                .setAuthEndpoint()
                .build()
        );

        JsonEndpoint.post(PATH, Token.class, (body, request, response) -> {

            UserRole role = SecurityFilter.userRoleAttr(request);
            Optional<UserSessionInfo> optHttpInfo = SecurityFilter.httpSessionInfo(request);

            if(role.equals(UserRole.UNAUTHENTICATED) || optHttpInfo.isEmpty()) {
                SecurityFilter.setUnauthorizedHeader(response);
                return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
            }

            Result<UserTokens.VerificationFailure, UserSessionInfo> verifyResult = UserTokens.verifyWithoutExpiration(
                body.token, UserTokenSubject.FOR_BEARER_TOKEN
            );

            if(verifyResult.isFailure()) {
                LOGGER.warn("Recieved invalid token for refresh request. Error is: {}", verifyResult.failureReason());
                return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
            }

            UserSessionInfo oldSession = verifyResult.result();

            if(!oldSession.toUserInfo().equals(optHttpInfo.get().toUserInfo())) {
                LOGGER.warn("Recieved refresh request with, cookie not matching body.");
                return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
            }

            if(!oldSession.sessionId.equals(optHttpInfo.get().sessionId)) {
                LOGGER.warn("Recieved refresh request with, cookie session ID not matching body session ID.");
                return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
            }

            String refreshedToken = UserTokens.sign(optHttpInfo.get(), UserTokenSubject.FOR_BEARER_TOKEN);
            return new Token(refreshedToken);
        });
    }

    private static final class Token implements JsonSerializable {
        public final String token;

        @ReflectiveConstructor
        private Token() {
            token = null;
        }

        public Token(String token) {
            this.token = token;
        }
    }
}
