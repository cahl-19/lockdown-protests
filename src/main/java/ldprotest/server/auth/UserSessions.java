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

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.time.ZonedDateTime;
import ldprotest.db.IndexTools;
import ldprotest.db.MainDatabase;
import ldprotest.main.Main;
import ldprotest.main.ServerTime;
import ldprotest.server.auth.UserAccount.UserAccountStatus;
import ldprotest.server.auth.UserAccount.UserAccountStatusCode;
import ldprotest.server.auth.UserAccount.UserLookupErrorCode;
import ldprotest.util.DateTools;
import ldprotest.util.ErrorCode;
import ldprotest.util.Result;
import org.bson.BsonDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserSessions {

    private static final String USER_SESSIONS_COLLECTION_NAME = "userSessions";
    private final static Logger LOGGER = LoggerFactory.getLogger(UserSessions.class);

    public static void setupDbIndex() {
        MongoCollection<UserSessionInfo> collection = collection();

        IndexTools.createIndexWithOpts(collection, Indexes.ascending("sessionId"), true, false);
        IndexTools.createIndexWithOpts(collection, Indexes.ascending("createdAt"), false, false);
    }

    public static Result<SessionCreationError, UserSessionInfo> createSession(UserInfo info) {
        MongoCollection<UserSessionInfo> collection = collection();
        UserSessionInfo session = UserSessionInfo.generateSession(info);

        Result<UserLookupErrorCode, UserAccountStatus> result = UserAccount.status(info);

        if(result.isFailure()) {
            return Result.failure(SessionCreationErrorFromUserLookupError(result.failureReason()));
        }

        if(result.result().code.equals(UserAccountStatusCode.ACCOUNT_LOCKED)) {
            return Result.failure(SessionCreationError.ACCOUNT_LOCKED);
        }

        try {
            collection.insertOne(session);
        } catch (MongoException ex) {
            LOGGER.error("Error writing new user session to the database", ex);
            return Result.failure(SessionCreationError.DATABASE_ERROR);
        }

        return Result.success(session);
    }

    public static Result<SessionCreationError, UserSessionInfo> refreshSession(UserSessionInfo sessionInfo) {
        ZonedDateTime now = ServerTime.now();
        MongoCollection<UserSessionInfo> collection = collection();
        Result<UserLookupErrorCode, UserAccountStatus> result = UserAccount.status(sessionInfo.email);

        if(result.isFailure()) {
            return Result.failure(SessionCreationErrorFromUserLookupError(result.failureReason()));
        }
        if(sessionInfo.expired()) {
            return Result.failure(SessionCreationError.EXPIRED);
        }
        if(result.result().code.equals(UserAccountStatusCode.ACCOUNT_LOCKED)) {
            return Result.failure(SessionCreationError.ACCOUNT_LOCKED);
        }

        try {
            UpdateResult updateResult = collection.updateOne(
                Filters.eq("sessionId", sessionInfo.sessionId),
                Updates.set("createdAt", DateTools.millisSinceEpoch(now))
            );

            if(updateResult.wasAcknowledged()) {
                return Result.success(sessionInfo.withNewCreationDate(now));
            } else {
                return Result.failure(SessionCreationError.NO_SUCH_ACOUNT);
            }

        } catch (MongoException ex) {
            LOGGER.error("Error refreshing session", ex);
            return Result.failure(SessionCreationError.DATABASE_ERROR);
        }
    }

    public static Result<SessionLookupError, UserSessionInfo> lookup(String sessionId) {
        MongoCollection<UserSessionInfo> collection = collection();

        try {
            UserSessionInfo session = collection.find(Filters.eq("sessionId", sessionId)).first();

            if(session == null) {
                return Result.failure(SessionLookupError.NO_SUCH_SESSION);
            } else {
                return Result.success(session);
            }

        } catch(MongoException ex) {
            return Result.failure(SessionLookupError.DATABASE_ERROR);
        }
    }

    public static ErrorCode<SessionDeleteError> deleteExpired() {
        MongoCollection<UserSessionInfo> collection = collection();
        ZonedDateTime expirePoint = ServerTime.now().minusSeconds(Main.args().sessionExpiresSeconds);
        long expirePointValue = expirePoint.toInstant().toEpochMilli();
        BsonDateTime threshold = new BsonDateTime(expirePointValue);

        try {
            collection.deleteMany(Filters.lt("createdAt", threshold));
        } catch(MongoException ex) {
            LOGGER.info("Exception thrown during deletion", ex);
            return ErrorCode.error(SessionDeleteError.DATABASE_ERROR);
        }

        return ErrorCode.success();
    }

    public static ErrorCode<SessionDeleteError> delete(String sessionId) {
        MongoCollection<UserSessionInfo> collection = collection();

        try {
            DeleteResult result = collection.deleteOne(Filters.eq("sessionId", sessionId));

            if(result.getDeletedCount() == 0) {
                return ErrorCode.error(SessionDeleteError.NO_SUCH_SESSION);
            } else {
                return ErrorCode.success();
            }

        } catch(MongoException ex) {
            return ErrorCode.error(SessionDeleteError.DATABASE_ERROR);
        }
    }

    private static SessionCreationError SessionCreationErrorFromUserLookupError(UserLookupErrorCode from) {
            switch(from) {
                case INVALID_USER:
                    return SessionCreationError.NO_SUCH_ACOUNT;
                case DATABASE_ERROR:
                    return SessionCreationError.DATABASE_ERROR;
                default:
                    LOGGER.error("Recieved unexpected status result: {}", from);
                    return SessionCreationError.DATABASE_ERROR;
            }
    }

    private static MongoCollection<UserSessionInfo> collection() {
        return MainDatabase.database().getCollection(USER_SESSIONS_COLLECTION_NAME, UserSessionInfo.class);
    }

    public static enum SessionCreationError {
        DATABASE_ERROR,
        ACCOUNT_LOCKED,
        NO_SUCH_ACOUNT,
        EXPIRED;
    }

    public static enum SessionLookupError {
        DATABASE_ERROR,
        NO_SUCH_SESSION;
    }

    public static enum SessionDeleteError {
        DATABASE_ERROR,
        NO_SUCH_SESSION;
    }
}
