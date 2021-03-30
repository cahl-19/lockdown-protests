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

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import ldprotest.business.PrivateProtestData;
import ldprotest.business.PublicProtestData;
import ldprotest.db.MongoErrorCode;
import ldprotest.geo.Coordinate;
import ldprotest.geo.GeoRectangle;
import ldprotest.serialization.JsonSerializable;
import ldprotest.server.auth.HttpVerbTypes;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.server.infra.JsonEndpoint;
import ldprotest.server.infra.JsonError;
import ldprotest.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoPin {

    private final static Logger LOGGER = LoggerFactory.getLogger(GeoPin.class);
    private static final String PATH = "/api/pins";

    private static final Pattern QUERY_PARAM_PATTERN = Pattern.compile(
        "^(-?[0-9]+\\.?[0-9]*),(-?[0-9]+\\.?[0-9]*)$"
    );

    private static final String SOUTH_WEST_QUERY_PARAM = "SW";
    private static final String NORTH_EAST_QUERY_PARAM = "NE";

    private static final int MAX_PROTESTS_PER_REQUEST = 128;

    private GeoPin() {
        /* GeoPin */
    }

    public static void register() {

        SecurityFilter.add(
            PATH,
            SecConfig.builder()
                .add(UserRole.MODERATOR, HttpVerbTypes.POST, HttpVerbTypes.GET)
                .add(UserRole.PLANNER, HttpVerbTypes.POST, HttpVerbTypes.GET)
                .add(UserRole.ADMIN, HttpVerbTypes.POST, HttpVerbTypes.GET)

                .add(UserRole.UNAUTHENTICATED, HttpVerbTypes.GET)
                .add(UserRole.USER, HttpVerbTypes.GET)

                .build()
        );

        JsonEndpoint.post(PATH, PublicProtestData.class, (protest, request, response) -> {

            Optional<UserSessionInfo> sessionInfo = SecurityFilter.bearerSessionInfo(request);

            if(sessionInfo.isEmpty()) {
                LOGGER.error(
                    "Unauthenticated request past the security filter. This means that the security filter has failed!"
                );
                return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
            }

            if(!protest.owner.orElse("").equals(sessionInfo.get().username)) {
                LOGGER.warn(
                    "Recieved protest creation request with owner!=username. owner={}, username={}.",
                    protest.owner, sessionInfo.get().username
                );
                return JsonEndpoint.responseFromError(JsonError.invalidBody("Owner does not match username"), response);
            }

            if(!protest.validate()) {
                LOGGER.warn("Recieved invalid protest object.");
                return JsonEndpoint.responseFromError(JsonError.invalidBody("Oversized"), response);
            }

            try {
                PrivateProtestData.collection().insertOne(
                    PrivateProtestData.generate(protest, sessionInfo.get().globalUniqueUserId)
                );
            } catch(MongoException ex) {
                LOGGER.error("Datbase error inserting protest", ex);
                return JsonEndpoint.responseFromError(JsonError.internalError(), response);
            }

            return JsonError.success();
        });

        JsonEndpoint.get(PATH, (request, response) -> {

            String swParam = request.queryParams(SOUTH_WEST_QUERY_PARAM);
            String neParam = request.queryParams(NORTH_EAST_QUERY_PARAM);

            if(swParam == null || neParam == null) {
                return JsonEndpoint.responseFromError(
                    JsonError.invalidParams("Missing required query param"), response
                );
            }

            Result<String, Coordinate> swResult = parseCoordinateQueryParam(swParam);
            Result<String, Coordinate> neResult = parseCoordinateQueryParam(neParam);

            if(swResult.isFailure() || neResult.isFailure()) {
                return JsonEndpoint.responseFromError(
                    JsonError.invalidParams("Coordinate param is invalid"), response
                );
            }

            try {
                return searchProtests(new GeoRectangle(swResult.result(), neResult.result()));
            } catch(MongoException ex) {
                if(ex.getCode() == MongoErrorCode.QUERY_OPTIONS_IN_ERROR.code) {
                    LOGGER.warn("Client queried for invalid region: sw={} ne={}", swResult.result(), neResult.result());
                    return new Protests(List.of());
                } else {
                    LOGGER.error("Database error when querying protests.", ex);
                    return JsonEndpoint.responseFromError(JsonError.internalError(), response);
                }
            }
        });
    }

    private static Result<String, Coordinate> parseCoordinateQueryParam(String param) {

        Matcher matcher = QUERY_PARAM_PATTERN.matcher(param);

        if(!matcher.matches()) {
            Result.failure("Parameter doesn't match pattern");
        }

        String latString = matcher.group(1);
        String longString = matcher.group(2);

        double lat = Double.parseDouble(latString);
        double lng = Double.parseDouble(longString);

        return Result.success(new Coordinate(lat, lng));
    }

    private static Protests searchProtests(GeoRectangle area) {
        MongoCollection<PrivateProtestData> collection = PrivateProtestData.collection();
        List<PrivateProtestData> protests = new ArrayList<>();

        for(PrivateProtestData data: collection.find(area.bsonFilter("location")).limit(MAX_PROTESTS_PER_REQUEST)) {
            protests.add(data);
        }

        return new Protests(protests);
    }

    private static final class Protests implements JsonSerializable {
        List<PrivateProtestData> protests;

        public Protests(List<PrivateProtestData> protests) {
            this.protests = protests;
        }
    }
}
