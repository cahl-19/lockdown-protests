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
import java.util.Optional;
import ldprotest.business.ProtestData;
import ldprotest.server.auth.HttpVerbTypes;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.server.infra.JsonEndpoint;
import ldprotest.server.infra.JsonError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeoPin {

    private final static Logger LOGGER = LoggerFactory.getLogger(GeoPin.class);
    private static final String PATH = "/api/pins";

    private GeoPin() {
        /* GeoPin */
    }

    public static void register() {

        SecurityFilter.add(
            PATH,
            SecConfig.builder()
                .add(UserRole.MODERATOR, HttpVerbTypes.POST)
                .add(UserRole.PLANNER, HttpVerbTypes.POST)
                .add(UserRole.ADMIN, HttpVerbTypes.POST)
                .build()
        );

        JsonEndpoint.post(PATH, ProtestData.class, (protest, request, response) -> {

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
                ProtestData.collection().insertOne(protest);
            } catch(MongoException ex) {
                LOGGER.error("Datbase error inserting protest", ex);
                return JsonEndpoint.responseFromError(JsonError.internalError(), response);
            }

            return JsonError.success();
        });
    }
}
