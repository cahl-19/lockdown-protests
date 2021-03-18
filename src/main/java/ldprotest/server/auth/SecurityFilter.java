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
package ldprotest.server.auth;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import ldprotest.server.auth.UserSessions.SessionCreationError;
import ldprotest.server.auth.webtoken.UserTokenSubject;
import ldprotest.server.auth.webtoken.UserTokens;
import ldprotest.server.auth.webtoken.UserTokens.VerificationFailure;
import ldprotest.server.endpoints.Login;
import ldprotest.server.infra.CookieAttributes;
import ldprotest.util.Result;
import ldprotest.util.types.UrlPathPrefixTree;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import static spark.Spark.before;
import static spark.Spark.halt;

public final class SecurityFilter {

    public final static SecurityFilter INSTANCE = new SecurityFilter();

    public static final String HTTP_AUTH_ATTRIBUTE = "userHttpAuth";
    public static final String BEARER_AUTH_ATTRIBUTE = "bearerAuth";
    public static final String SEC_CONFIG_ATTRIBUTE = "secConfig";
    public static final String USER_ROLE_ATTRIBUTE = "userRole";
    public static final String HTTP_VERB_ATTRIBUTE = "httpVerb";

    private final static Logger LOGGER = LoggerFactory.getLogger(SecurityFilter.class);

    private final UrlPathPrefixTree<FilterData> authTree;

    private SecurityFilter() {
        this.authTree = new UrlPathPrefixTree<>();
    }

    private synchronized void addMethod(String path, SecConfig config) {

        List<String> elements = UrlPathPrefixTree.splitPath(path);

        if(elements.isEmpty()) {
            throw new SecurityConfigException("Empty path not allowed");
        }

        if(!elements.get(0).equals("/")) {
            throw new SecurityConfigException("Path must start with /");
        }

        if(elements.stream().skip(1).anyMatch((elem) -> elem.isEmpty())) {
            LOGGER.info("path {}", path);
            throw new SecurityConfigException("Empty path element not alllowed");
        }

        if(elements.stream().anyMatch((elem) -> elem.contains("*") && !(elem.equals("*") || elem.equals("**")))) {
            throw new SecurityConfigException("Wildcard character '*' not permitted except as standalone '*' or '**'");
        }

        long wildCount = elements.stream().filter((elem) -> elem.equals("*")).count();

        if(wildCount > 1) {
            throw new SecurityConfigException("Only one wildcard element alllowed.");
        }

        String last = elements.get(elements.size() - 1);

        if(wildCount == 1 && !last.contains("*")) {
            throw new SecurityConfigException("Only last path element may be wild.");
        }

        if(authTree.lookup(elements).isPresent()) {
            throw new SecurityConfigException("Security configuration may not be set twice for the same path");
        }

        if(last.equals("*")) {
            elements.remove(elements.size() - 1);
            authTree.add(elements, new FilterData(path, Wildness.WILD, config));
        } else if(last.equals("**")) {
            elements.remove(elements.size() - 1);
            authTree.add(elements, new FilterData(path, Wildness.DOUBLE_WILD, config));
        } else {
            authTree.add(elements, new FilterData(path, Wildness.NOT_WILD, config));
        }
    }

    private Optional<FilterData> lookup(String path) {
        return authTree.longest(path);
    }

    public static void add(String path, SecConfig config) {
        INSTANCE.addMethod(path, config);
    }

    public static void start() {
        before((request, response) -> {
            FilterData data = getFilterDataOrHalt(request.pathInfo());

            HttpVerbTypes method = HttpVerbTypes.fromString(request.requestMethod());

            Optional<String> cookieToken = optCookieValue(request.cookie(Login.LOGIN_COOKIE_NAME));
            Optional<String> bearerToken = extractBearer(request);

            setupUserAuth(cookieToken, bearerToken);

            AuthInfo auth = setupUserAuth(cookieToken, bearerToken);

            if(!data.secConfig.isPermitted(auth.role, method)) {
                LOGGER.warn("Unauthorized request from role {} for {} {}", auth.role, method, request.pathInfo());
                setUnauthorizedHeader(response);

                throw halt(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
            }

            request.attribute(SEC_CONFIG_ATTRIBUTE, data.secConfig);
            request.attribute(USER_ROLE_ATTRIBUTE, auth.role);
            request.attribute(HTTP_VERB_ATTRIBUTE, method);
            auth.setAttributes(request);
            auth.setCookies(response);
        });
    }

    public static void setUnauthorizedHeader(Response response) {
        response.header("WWW-Authenticate", "Bearer realm=API, charset=UTF-8");
    }

    public static SecConfig secConfigAttr(Request request) {
        return request.attribute(SEC_CONFIG_ATTRIBUTE);
    }

    public static UserRole userRoleAttr(Request request) {
        return request.attribute(USER_ROLE_ATTRIBUTE);
    }

    public static HttpVerbTypes httpVerbAttribute(Request request) {
        return request.attribute(HTTP_VERB_ATTRIBUTE);
    }

    public static Optional<UserSessionInfo> httpSessionInfo(Request request) {
        return Optional.ofNullable(request.attribute(HTTP_AUTH_ATTRIBUTE));
    }

    public static Optional<UserSessionInfo> bearerSessionInfo(Request request) {
        return Optional.ofNullable(request.attribute(BEARER_AUTH_ATTRIBUTE));
    }

    private static Optional<String> extractBearer(Request request) {
        String header = request.headers("Authorization");

        if(header == null) {
            return Optional.empty();
        }

        String[] parts = header.split(" ");

        if(parts.length != 2 || !parts[0].equals("Bearer")) {
            LOGGER.warn("Invalid  authorization header schema. Header text: {}", header);
            return Optional.empty();
        }

        return Optional.of(parts[1]);
    }

    private static AuthInfo setupUserAuth(
        Optional<String> cookieToken, Optional<String> bearerToken
    ) {
        Map<String, Object> attributes = new HashMap<>();
        Map<String, CookieAttributes> cookies = new HashMap<>();

        Optional<UserSessionInfo> cookieSession = Optional.empty();
        Optional<UserSessionInfo> bearerSession = Optional.empty();

        if(cookieToken.isEmpty() && bearerToken.isEmpty()) {
            return new AuthInfo(UserRole.UNAUTHENTICATED, attributes, cookies);
        }

        if(cookieToken.isPresent()) {
            Result<VerificationFailure, UserSessionInfo> result = UserTokens.verify(
                cookieToken.get(), UserTokenSubject.FOR_COOKIE
            );

            if(result.isFailure()) {
                if(result.failureReason().equals(VerificationFailure.EXPIRED)) {
                    cookieSession = attemptRefresh(cookieToken.get(), UserTokenSubject.FOR_COOKIE);
                    if(cookieSession.isPresent()) {
                        String refreshedToken = UserTokens.sign(cookieSession.get(), UserTokenSubject.FOR_COOKIE);
                        cookies.put(
                            Login.LOGIN_COOKIE_NAME,
                            Login.loginTokenCookie(refreshedToken)
                        );
                    }
                } else if(result.failureReason().equals(VerificationFailure.NO_KEY)){
                    cookies.put(Login.LOGIN_COOKIE_NAME, Login.deleteTokenCookie());
                } else {
                    processVerificationFailure(result.failureReason());
                }
            } else {
                cookieSession = Optional.of(result.result());
            }
        }

        if(cookieSession.isPresent()) {
            attributes.put(HTTP_AUTH_ATTRIBUTE, cookieSession.get());
        }

        if(bearerToken.isPresent()) {
            Result<VerificationFailure, UserSessionInfo> result = UserTokens.verify(
                bearerToken.get(), UserTokenSubject.FOR_BEARER_TOKEN
            );
            if(result.isFailure()) {
                processVerificationFailure(result.failureReason());
            } else {
                bearerSession = Optional.of(result.result());
            }
        }

        if(bearerSession.isPresent()) {
            attributes.put(BEARER_AUTH_ATTRIBUTE, bearerSession.get());
        }

        if(cookieSession.isPresent() && bearerSession.isPresent()) {
            if(!cookieSession.get().toUserInfo().equals(bearerSession.get().toUserInfo())) {
                LOGGER.warn((
                        "User info in cookie token does not match info in header token. This may be a sign of " +
                        "malicious activity"
                ));
                cookies.put(Login.LOGIN_COOKIE_NAME, Login.deleteTokenCookie());
                return new AuthInfo(UserRole.UNAUTHENTICATED, attributes, cookies);
            }
            if(!cookieSession.get().sessionId.equals(bearerSession.get().sessionId)) {
                LOGGER.warn((
                        "Session ID in cookie token does not match info in header token. This may be a sign of " +
                        "malicious activity"
                ));
                cookies.put(Login.LOGIN_COOKIE_NAME, Login.deleteTokenCookie());
                return new AuthInfo(UserRole.UNAUTHENTICATED, attributes, cookies);
            }
        }

        if(cookieSession.isPresent()) {
            return new AuthInfo(cookieSession.get().role, attributes, cookies);
        } else if(bearerSession.isPresent()) {
            return new AuthInfo(bearerSession.get().role, attributes, cookies);
        } else {
            return new AuthInfo(UserRole.UNAUTHENTICATED, attributes, cookies);
        }
    }

    private static void processVerificationFailure(VerificationFailure failure) {
        switch(failure) {
            case EXPIRED:
                break;
            case NO_KEY:
                LOGGER.warn("No key for token.");
            case INVALID_CLAIM:
            case SIGNATURE_FAILED:
            case OTHER_ERROR:
            case MALFORMED_TOKEN:
                LOGGER.warn(
                    "Malformed or unverifiable token. This may be a sign of malicious activity. Error: {}", failure
                );
                throw halt(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
            default:
                LOGGER.error("Unknown failure code: {}", failure);
                throw halt(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
        }
    }

    private static Optional<UserSessionInfo> attemptRefresh(String oldToken, UserTokenSubject subject) {

        Result<VerificationFailure, UserSessionInfo> result = UserTokens.verifyWithoutExpiration(
            oldToken, subject
        );

        if(result.isFailure()) {
            LOGGER.warn(
                "Invalid token submitted. This may be a sign of malicious activity. Error result is: {}",
                result.failureReason()
            );
            throw halt(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
        }

        UserSessionInfo oldSession = result.result();

        Result<SessionCreationError, UserSessionInfo> err = UserSessions.refreshSession(oldSession);

        if(err.isFailure()) {
            return Optional.empty();
        } else {
            return Optional.of(err.result());
        }
    }

    private static FilterData getFilterDataOrHalt(String path) {
            Optional<FilterData> optData = INSTANCE.lookup(path);

            if(optData.isEmpty()) {
                LOGGER.error("No security data found for path: {}", path);
                throw halt(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
            }

            FilterData data = optData.get();

            if(data.notWild() && path.equals(data.pathSpec)) {
                return data;
            } else if(data.singleWild() && prefix(data.pathSpec).equals(prefix(path))) {
                return data;
            } else if(data.doubleWild()) {
                return data;
            }

            LOGGER.warn("Request for unconfigured subdirectory. Full request path: {}", path);
            throw halt(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
    }

    private static Optional<String> optCookieValue(String value) {
        if(value == null || value.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(value);
        }
    }

    private static String prefix(String path) {
        return path.substring(0, path.lastIndexOf("/"));
    }

    private static enum Wildness {
        WILD,
        DOUBLE_WILD,
        NOT_WILD;
    }

    private static class AuthInfo {

        public final UserRole role;

        private final Map<String, Object> attributes;
        private final Map<String, CookieAttributes> cookies;

        public AuthInfo(UserRole role, Map<String, Object> attributes, Map<String, CookieAttributes> cookies) {
            this.role = role;
            this.attributes = attributes;
            this.cookies = cookies;
        }

        public void setAttributes(Request request) {
            for(Entry<String, Object> ent: attributes.entrySet()) {
                request.attribute(ent.getKey(), ent.getValue());
            }
        }

        public void setCookies(Response response) {
            for(Entry<String, CookieAttributes> ent: cookies.entrySet()) {
                ent.getValue().setCookie(response);
            }
        }
    }

    private static class FilterData {

        public final String pathSpec;
        public final Wildness wildness;
        public final SecConfig secConfig;

        public FilterData(String pathSpec, Wildness wildness, SecConfig secConfig) {
            this.pathSpec = pathSpec;
            this.wildness = wildness;
            this.secConfig = secConfig;
        }

        public boolean notWild() {
            return wildness == Wildness.NOT_WILD;
        }

        public boolean singleWild() {
            return wildness == Wildness.WILD;
        }

        public boolean doubleWild() {
            return wildness == Wildness.DOUBLE_WILD;
        }
    }
}
