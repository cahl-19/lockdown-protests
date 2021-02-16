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
import ldprotest.server.auth.HttpVerbTypes;
import ldprotest.server.auth.UserAccount;
import ldprotest.server.auth.UserAccount.UserLookupError;
import ldprotest.server.auth.UserInfo;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.server.auth.UserSessions;
import ldprotest.server.auth.UserSessions.SessionCreationError;
import ldprotest.server.auth.webtoken.UserTokenSubject;
import ldprotest.server.auth.webtoken.UserTokens;
import ldprotest.server.infra.CookieAttributes;
import ldprotest.server.infra.JsonEndpoint;
import ldprotest.server.infra.JsonError;
import ldprotest.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Login {

    public static final String LOGIN_COOKIE_NAME = "login-token";

    private static final String PATH = "/api/login";
    private final static Logger LOGGER = LoggerFactory.getLogger(Login.class);

    private Login() {
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

        JsonEndpoint.post(PATH, LoginJson.class, (loginData, request, response) -> {
            Result<UserLookupError, UserInfo> result = UserAccount.authenticate(loginData.username, loginData.password);

            if(result.isFailure()) {
                return JsonEndpoint.responseFromError(JsonError.loginError(), response);
            }

            Result<SessionCreationError, UserSessionInfo> sessionResult = UserSessions.createSession(result.result());

            if(sessionResult.isFailure()) {
                return JsonEndpoint.responseFromError(JsonError.loginError(), response);
            }

            String cookieToken = UserTokens.sign(sessionResult.result(), UserTokenSubject.FOR_COOKIE);
            String headerToken = UserTokens.sign(sessionResult.result(), UserTokenSubject.FOR_BEARER_TOKEN);

            CookieAttributes cookieAtr = loginTokenCookie(cookieToken, usingHttps);

            cookieAtr.setCookie(response);

            return new Token(headerToken);
        });
    }

    public static CookieAttributes loginTokenCookie(String value) {
        return loginTokenCookie(value, Main.args().usingHttps);
    }

    public static CookieAttributes loginTokenCookie(String value, boolean usingHttps) {
        return new CookieAttributes("/", LOGIN_COOKIE_NAME, value, UserTokens.KEY_EXPIRY_SECONDS, usingHttps, true);
    }

    public static CookieAttributes deleteTokenCookie() {
        return deleteTokenCookie(Main.args().usingHttps);
    }

    public static CookieAttributes deleteTokenCookie(boolean usingUttps) {
        return new CookieAttributes("/", LOGIN_COOKIE_NAME, "", -1, usingUttps, true);
    }

    private static final class LoginJson implements JsonSerializable {
        public final String username;
        public final String password;

        @ReflectiveConstructor
        private LoginJson() {
            username = "";
            password = "";
        }
    }

    private static final class Token implements JsonSerializable {
        private final String token;

        public Token(String token) {
            this.token = token;
        }
    }
}
