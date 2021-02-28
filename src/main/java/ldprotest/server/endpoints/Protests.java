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
import java.util.UUID;
import ldprotest.business.PrivateProtestData;
import ldprotest.business.PublicProtestData;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.server.auth.HttpVerbTypes;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.server.infra.JsonEndpoint;
import ldprotest.server.infra.JsonError;
import ldprotest.util.types.MayFail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.Request;

public final class Protests {

    private final static Logger LOGGER = LoggerFactory.getLogger(Protests.class);
    private static final String PATH = "/api/protests/*";

    private Protests() {
        /* GeoPin */
    }

    public static void register() {

        SecurityFilter.add(
            PATH,
            SecConfig.builder()
                .add(UserRole.MODERATOR, HttpVerbTypes.POST, HttpVerbTypes.DELETE)
                .add(UserRole.PLANNER, HttpVerbTypes.POST, HttpVerbTypes.DELETE)
                .add(UserRole.ADMIN, HttpVerbTypes.POST, HttpVerbTypes.DELETE)

                .build()
        );

        registerEdit();
        registerDelete();
    }

    private static void registerEdit() {

        JsonEndpoint.post(PATH, PublicProtestData.class, (protest, request, response) -> {

            Optional<UserSessionInfo> sessionInfo = SecurityFilter.bearerSessionInfo(request);
            UserRole role = SecurityFilter.userRoleAttr(request);
            MayFail<String> protestIdField = getRequiredSplat(request, 0);

            if(sessionInfo.isEmpty()) {
                LOGGER.error(
                    "Unauthenticated request past the security filter. This means that the security filter has failed!"
                );
                return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
            }

            UUID userId = sessionInfo.get().globalUniqueUserId;
            String username = sessionInfo.get().username;

            if(protestIdField.isFailure()) {
                LOGGER.warn("Missing required protest ID url path element");
                return JsonEndpoint.responseFromError(JsonError.invalidParams("missing url protest ID"), response);
            }

            MayFail<UUID> protestIdMayFail = MayFail.succeedOrEatException(
                IllegalArgumentException.class, () -> UUID.fromString(protestIdField.result())
            );

            if(protestIdMayFail.isFailure()) {
                LOGGER.warn("Invalid protest ID format");
                return JsonEndpoint.responseFromError(JsonError.invalidParams("invalid protest ID"), response);
            }

            if(!protest.validate()) {
                LOGGER.warn("Recieved invalid protest object.");
                return JsonEndpoint.responseFromError(JsonError.invalidBody("Oversized"), response);
            }

            PrivateProtestData orig;
            try {
                orig = PrivateProtestData.lookupByProtestId(protestIdMayFail.result());
            } catch(MongoException ex) {
                LOGGER.error("Datbase error looking up protest", ex);
                return JsonEndpoint.responseFromError(JsonError.internalError(), response);
            }

            if(orig == null) {
                LOGGER.warn("Attempt by {} to modify non-existant protest", userId);
                return JsonEndpoint.responseFromError(JsonError.invalidParams("Protest Not Found"), response);
            }

            if(!(role.equals(UserRole.ADMIN) || role.equals(UserRole.MODERATOR))) {
                if(!orig.owner.get().equals(username) || !orig.ownerId.equals(userId)) {
                    LOGGER.error("Attempt by {} {} to modify a protest they don't own", username, userId);
                    return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
                }
            }

            try {
                PrivateProtestData.updateProtest(
                    PrivateProtestData.publicToPrivate(protest, orig.ownerId, orig.protestId)
                );
            } catch(MongoException ex) {
                LOGGER.error("Datbase error updating protest", ex);
                return JsonEndpoint.responseFromError(JsonError.internalError(), response);
            }

            return JsonError.success();
        });
    }

    private static void registerDelete() {
        JsonEndpoint.delete(PATH, EmptyJsonDoc.class, (body, request, response) -> {

            Optional<UserSessionInfo> sessionInfo = SecurityFilter.bearerSessionInfo(request);
            UserRole role = SecurityFilter.userRoleAttr(request);
            MayFail<String> protestIdField = getRequiredSplat(request, 0);

            if(sessionInfo.isEmpty()) {
                LOGGER.error(
                    "Unauthenticated request past the security filter. This means that the security filter has failed!"
                );
                return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
            }

            UUID userId = sessionInfo.get().globalUniqueUserId;
            String username = sessionInfo.get().username;

            if(protestIdField.isFailure()) {
                LOGGER.warn("Missing required protest ID url path element");
                return JsonEndpoint.responseFromError(JsonError.invalidParams("missing url protest ID"), response);
            }

            MayFail<UUID> protestIdMayFail = MayFail.succeedOrEatException(
                IllegalArgumentException.class, () -> UUID.fromString(protestIdField.result())
            );

            if(protestIdMayFail.isFailure()) {
                LOGGER.warn("Invalid protest ID format");
                return JsonEndpoint.responseFromError(JsonError.invalidParams("invalid protest ID"), response);
            }

            PrivateProtestData orig;
            try {
                orig = PrivateProtestData.lookupByProtestId(protestIdMayFail.result());
            } catch(MongoException ex) {
                LOGGER.error("Datbase error looking up protest", ex);
                return JsonEndpoint.responseFromError(JsonError.internalError(), response);
            }

            if(orig == null) {
                LOGGER.warn("Attempt by {} to modify non-existant protest", userId);
                return JsonEndpoint.responseFromError(JsonError.invalidParams("Protest Not Found"), response);
            }

            if(!(role.equals(UserRole.ADMIN) || role.equals(UserRole.MODERATOR))) {
                if(!orig.owner.get().equals(username) || !orig.ownerId.equals(userId)) {
                    LOGGER.error("Attempt by {} {} to delete a protest they don't own", username, userId);
                    return JsonEndpoint.responseFromError(JsonError.unauthorizedError(), response);
                }
            }

            try {
                PrivateProtestData.deleteProtest(orig.protestId);
            } catch(MongoException ex) {
                LOGGER.error("Datbase error deleting protest", ex);
                return JsonEndpoint.responseFromError(JsonError.internalError(), response);
            }

            return JsonError.success();
        });
    }

    private static MayFail<String> getRequiredSplat(Request request, int index) {
        String[] fields = request.splat();

        if(index >= fields.length) {
            return MayFail.failure();
        }

        String s = fields[index];

        if(s == null || s.isEmpty()) {
            return MayFail.failure();
        }

        return MayFail.success(s);
    }

    private static final class EmptyJsonDoc implements JsonSerializable {
        @ReflectiveConstructor
        private EmptyJsonDoc() {

        }
    }
}
