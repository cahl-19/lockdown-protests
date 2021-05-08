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

import com.mongodb.DuplicateKeyException;
import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.UpdateResult;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import ldprotest.db.DbIndex;
import ldprotest.db.IndexTools;
import ldprotest.db.MainDatabase;
import ldprotest.db.codec.UUIDCodec;
import ldprotest.main.ServerTime;
import ldprotest.serialization.BsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.util.Result;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AuthFailureLockout {

    private final static Logger LOGGER = LoggerFactory.getLogger(AuthFailureLockout.class);

    private static final long RESET_PERIOD_BASE_SECONDS = 10 * 60;
    private static final int RESET_MAX_INTERVALS = 4;
    private static final int FAILURE_COUNT_TRIGGER_THRESHOLD = 5;

    private static final String AUTH_FAILURE_COLLECTION = "authFailure";

    public static Result<RecordFailureError, AuthFailureResult> recordFailure(UUID uid) {
        ZonedDateTime now = ServerTime.now();
        try {
            AuthFailureRecord record = incrementFailure(uid, now);

            if((record.count % FAILURE_COUNT_TRIGGER_THRESHOLD) == 0) {
                return Result.success(new AuthFailureResult(now.plusSeconds(record.resetPeriodSeconds() / 2)));
            } else {
                return Result.success(new AuthFailureResult());
            }

        } catch(MongoException ex) {
            LOGGER.error("Database Error while recording  auth failure", ex);
            return Result.failure(RecordFailureError.DATABASE_ERROR);
        }
    }

    public static void clearFailures(UUID uid) {

        MongoCollection<AuthFailureRecord> collection = collection();

        try {
            collection.deleteOne(findFilter(uid));
        } catch(MongoException ex) {
            LOGGER.error("Error while clearing auth failures.");
        }
    }

    public static void setupDbIndex() {
        Collection<Bson> indexes = IndexTools.reflectiveBuildIndexes(AuthFailureResult.class);

        for(Bson index: indexes) {
            IndexTools.createIndexWithOpts(collection(), index, true, false);
        }
    }

    private static AuthFailureRecord incrementFailure(UUID uid, ZonedDateTime now) {
        MongoCollection<AuthFailureRecord> collection = collection();

        Bson origFilter = findFilter(uid);
        do {
            AuthFailureRecord orig = collection.find(origFilter).first();
            AuthFailureRecord next = orig == null ? new AuthFailureRecord(uid, now) : orig.countFailure(now);

            if(orig == null) {
                try {
                    collection.insertOne(next);
                } catch(DuplicateKeyException ex) {
                    continue;
                }
                return next;
            } else {
                Bson nextFilter = Filters.and(origFilter, Filters.eq("sequence", orig.sequence));
                ReplaceOptions options = new ReplaceOptions();

                options.upsert(false);
                UpdateResult result = collection.replaceOne(nextFilter, next, options);

                if(result.getMatchedCount() != 1) {
                    continue;
                }

                return next;
            }
        } while(true);
    }

    private static Bson findFilter(UUID uid) {
        return Filters.eq("uid", UUIDCodec.toBsonValue((uid)));
    }

    private static MongoCollection<AuthFailureRecord> collection() {
        return MainDatabase.database().getCollection(AUTH_FAILURE_COLLECTION, AuthFailureRecord.class);
    }

    public enum RecordFailureError {
        DATABASE_ERROR;
    }

    public static final class AuthFailureResult {
        public final Optional<ZonedDateTime> lockUntil;

        private AuthFailureResult(ZonedDateTime lockUntil) {
            this.lockUntil = Optional.of(lockUntil);
        }

        public AuthFailureResult() {
            this.lockUntil = Optional.empty();
        }

        public boolean mustLock() {
            return lockUntil.isPresent();
        }
    }

    private static final class AuthFailureRecord implements BsonSerializable {

        private final long sequence;

        @DbIndex
        public final UUID uid;

        public final ZonedDateTime itime;
        public final ZonedDateTime mtime;

        public final int count;

        @ReflectiveConstructor
        private AuthFailureRecord() {
            sequence = 0;
            uid = null;

            itime = null;
            mtime = null;

            count = 0;
        }

        private AuthFailureRecord(
            long sequence, UUID uid, ZonedDateTime itime, ZonedDateTime mtime, int count
        ) {
            this.sequence = sequence;
            this.uid = uid;
            this.itime = itime;
            this.mtime = mtime;
            this.count = count;
        }

        public AuthFailureRecord(UUID uid, ZonedDateTime time) {
            this.sequence = 0;
            this.uid = uid;
            this.itime = time;
            this.mtime = time;
            this.count = 1;
        }

        public AuthFailureRecord countFailure(ZonedDateTime time) {
            if(mtime.plusSeconds(resetPeriodSeconds()).isBefore(time)) {
                return new AuthFailureRecord(
                    sequence + 1,
                    uid,
                    time,
                    time,
                    1
                );
            } else {
                return new AuthFailureRecord(
                    sequence + 1,
                    uid,
                    itime,
                    time,
                    count + 1
                );
            }
        }

        public long resetPeriodSeconds() {
            int intervals = count / FAILURE_COUNT_TRIGGER_THRESHOLD;
            return RESET_PERIOD_BASE_SECONDS * (1 << Math.min(RESET_MAX_INTERVALS, intervals));
        }
    }
}
