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
package ldprotest.business.mapbox;

import com.google.gson.JsonParseException;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.nio.charset.Charset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import ldprotest.main.ServerTime;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.JsonSerialization;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.util.Result;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MapboxTokenApi {

    private final static Logger LOGGER = LoggerFactory.getLogger(MapboxTokenApi.class);

    private static final String TEMPORARY_TOKEN_BASE_URL = "https://api.mapbox.com/tokens/v2/";
    private static final Charset REQUEST_CHARSET = Charset.forName("UTF-8");

    private static final List<String> PUBLIC_TOKEN_SCOPES = Arrays.asList(
        "styles:tiles", "styles:read", "fonts:read", "datasets:read", "vision:read"
    );

    public static Result<ApiFailure, TemporaryTokenResponse> createTemporaryToken(
        String username, String accessToken, int expiresSeconds
    ) {
        HttpResponse<String> response;
        ZonedDateTime expiresAt = ServerTime.now().plusSeconds(expiresSeconds);
        TemporaryTokenRequest requestBody = new TemporaryTokenRequest(expiresAt, PUBLIC_TOKEN_SCOPES);

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder(
            URI.create(TEMPORARY_TOKEN_BASE_URL + username + "?access_token=" + accessToken)
        )
            .header("accept", "application/json")
            .header("Content-Type", "application/json")
            .POST(BodyPublishers.ofString(JsonSerialization.GSON.toJson(requestBody)))
            .build();

        try {
           response = client.send(request, BodyHandlers.ofString(REQUEST_CHARSET));
        } catch(IOException ex) {
            LOGGER.warn("IOError refreshing mapbox token: {}", ex.getMessage());
            return Result.failure(ApiFailure.IOERROR);
        } catch(InterruptedException ex) {
            LOGGER.warn("Interrupted while refreshing mapbox token");
            return Result.failure(ApiFailure.INTERRUPTED);
        }

        if(!statusOkay(response.statusCode())) {
            if(response.statusCode() == HttpStatus.UNAUTHORIZED_401) {
                LOGGER.warn("mapbox API returned unauthorized on temporary token creation request");
                return Result.failure(ApiFailure.UNEXPECTED_RESPONSE_CODE);
            } else {
                LOGGER.warn("mapbox API returned status code {}", response.statusCode());
                LOGGER.warn("mapbox API body response {}", response.body());
                return Result.failure(ApiFailure.UNAUTHORIZED);
            }
        }

        try {
            return Result.success(JsonSerialization.GSON.fromJson(response.body(), TemporaryTokenResponse.class));
        }  catch(JsonParseException e) {
            LOGGER.error("Unexpected mapbox create token response body: {}", response.body());
           return Result.failure(ApiFailure.UNEXPECTED_RESPONSE_BODY);
        }
    }

    private static boolean statusOkay(int status) {
        return status == HttpStatus.OK_200 || status == HttpStatus.CREATED_201 || status == HttpStatus.ACCEPTED_202;
    }

    private static final class TemporaryTokenRequest implements JsonSerializable {
        public final String expires;
        public final List<String> scopes;

        public TemporaryTokenRequest(ZonedDateTime expires, List<String> scopes) {
            this.expires = expires.format(DateTimeFormatter.ISO_INSTANT);
            this.scopes = scopes;
        }
    }

    public static final class TemporaryTokenResponse implements JsonSerializable {
        public final String token;

        @ReflectiveConstructor
        private TemporaryTokenResponse() {
            token = null;
        }
    }

    public enum ApiFailure {
        UNEXPECTED_RESPONSE_BODY,
        UNEXPECTED_RESPONSE_CODE,
        UNAUTHORIZED,
        INTERRUPTED,
        IOERROR;
    }
}
