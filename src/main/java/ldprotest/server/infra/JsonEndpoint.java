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
package ldprotest.server.infra;

import com.google.gson.JsonParseException;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.JsonSerialization;
import ldprotest.server.auth.HttpVerbTypes;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.server.infra.http.ContentType;
import ldprotest.util.ErrorCode;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;
import spark.Response;
import spark.Spark;

public final class JsonEndpoint {

    private final static Logger LOGGER = LoggerFactory.getLogger(JsonEndpoint.class);

    public static final ContentType CONTENT_TYPE = ContentType.parse("application/json; charset=UTF-8");

    private static final Map<Integer, Integer> ERROR_TO_DEFAULT_STATUS_CODE_MAP =
        Collections.unmodifiableMap(Map.<Integer, Integer>of(
            JsonError.ServerErrorCode.SUCCESS.code(), HttpStatus.OK_200,
            JsonError.ServerErrorCode.GENERIC_INTERNAL_ERROR.code(), HttpStatus.INTERNAL_SERVER_ERROR_500,
            JsonError.ServerErrorCode.CONTENT_TYPE_ERROR.code(), HttpStatus.BAD_REQUEST_400,
            JsonError.ServerErrorCode.LOGIN_FAILURE.code(), HttpStatus.UNAUTHORIZED_401,
            JsonError.ServerErrorCode.UNAUTHORIZED_FAILURE.code(), HttpStatus.UNAUTHORIZED_401,
            JsonError.ServerErrorCode.INVALID_REQUEST_BODY.code(), HttpStatus.BAD_REQUEST_400,
            JsonError.ServerErrorCode.INVALID_REQUEST_PARAMS.code(), HttpStatus.BAD_REQUEST_400
        ));

    public static void get(String url, JsonGetRoute route) {
        Spark.get(url, (request, response) -> {

            ErrorCode<JsonError> secCheckResult = secCheck(request, response);

            if(secCheckResult.failed()) {
                return JsonSerialization.GSON.toJson(responseFromError(secCheckResult.reason(), response));
            }

            JsonSerializable body = route.handle(request, response);
            return returnJsonResponse(body, response);
        });
    }

    public static <T extends JsonSerializable> void post(String url, Class<T> clazz, JsonDataRoute<T> route) {
        Spark.post(url, (request, response) -> {
            return dataRequest(request, response, clazz, route);
        });
    }

    public static <T extends JsonSerializable> void put(String url, Class<T> clazz, JsonDataRoute<T> route) {
        Spark.put(url, (request, response) -> {
            return dataRequest(request, response, clazz, route);
        });
    }

    public static <T extends JsonSerializable> void delete(String url, Class<T> clazz, JsonDataRoute<T> route) {
        Spark.delete(url, (request, response) -> {
            return dataRequest(request, response, clazz, route);
        });
    }

    public static JsonSerializable responseFromError(JsonError error, Response response) {
        int status = ERROR_TO_DEFAULT_STATUS_CODE_MAP.getOrDefault(error.code, HttpStatus.INTERNAL_SERVER_ERROR_500);
        return setReturn(error, response, status);
    }

    public static JsonSerializable setReturn(JsonSerializable body, Response response) {
        return body;
    }

    public static JsonSerializable setReturn(JsonSerializable body, Response response, int errorCode) {
        response.status(errorCode);
        return body;
    }

    private static <T extends JsonSerializable> String dataRequest(
        Request request, Response response, Class<T> clazz, JsonDataRoute<T> route
    ) {
        response.header("Content-Type", CONTENT_TYPE.toString());
        try {
            ContentType requestContentType = ContentType.parse(request);

            if(!CONTENT_TYPE.equals(requestContentType)) {
                return JsonSerialization.GSON.toJson(responseFromError(JsonError.contentTypeError(), response));
            }
        } catch(ContentType.ContentTypeError e) {
            LOGGER.warn("Received request with invalid content type: {}", e.getMessage());
            return JsonSerialization.GSON.toJson(responseFromError(JsonError.contentTypeError(), response));
        }

        ErrorCode<JsonError> secCheckResult = secCheck(request, response);
        if(secCheckResult.failed()) {
            return JsonSerialization.GSON.toJson(responseFromError(secCheckResult.reason(), response));
        }

        T data;
        try {
            data = JsonSerialization.GSON.fromJson(request.body(), clazz);
        } catch(NumberFormatException ex) {
            return JsonSerialization.GSON.toJson(
                responseFromError(JsonError.invalidBody("Invalid JSON or violated schema"), response)
            );
        }
        if(data == null) {
            return JsonSerialization.GSON.toJson(
                responseFromError(JsonError.invalidBody("Invalid JSON or violated schema"), response)
            );
        }

        JsonSerializable body = route.handle(data, request, response);

        return returnJsonResponse(body, response);
    }

    private static String returnJsonResponse(JsonSerializable body, Response response) {
        response.header("Content-Type", CONTENT_TYPE.toString());

        try {
           return JsonSerialization.GSON.toJson(setReturn(body, response));
        } catch(JsonParseException e) {
           return JsonSerialization.GSON.toJson(responseFromError(JsonError.internalError(), response));
        }
    }

    private static ErrorCode<JsonError> secCheck(Request request, Response response) {

        SecConfig conf = SecurityFilter.secConfigAttr(request);
        HttpVerbTypes method = SecurityFilter.httpVerbAttribute(request);
        UserRole role = SecurityFilter.userRoleAttr(request);

        boolean unauthenticated_permitted = conf.isPermitted(UserRole.UNAUTHENTICATED, method);

        if(unauthenticated_permitted && role.equals(UserRole.UNAUTHENTICATED)) {
            return ErrorCode.success();
        }
        if(unauthenticated_permitted && conf.isAuthEndpoint()) {
            return ErrorCode.success();
        }

        Optional<UserSessionInfo> optBearerInfo = SecurityFilter.bearerSessionInfo(request);
        Optional<UserSessionInfo> optCookieInfo = SecurityFilter.httpSessionInfo(request);

        if(optBearerInfo.isEmpty() || optCookieInfo.isEmpty()) {
            SecurityFilter.setUnauthorizedHeader(response);
            return ErrorCode.error(JsonError.unauthorizedError());
        } else {
            return ErrorCode.success();
        }
    }

    public static interface JsonGetRoute {
        JsonSerializable handle(Request request, Response response);
    }

    public static interface JsonDataRoute<T extends JsonSerializable> {
        JsonSerializable handle(T data, Request request, Response response);
    }
}
