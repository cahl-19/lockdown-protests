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
package ldprotest.business;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.UUID;
import ldprotest.db.IndexTools;
import ldprotest.db.MainDatabase;
import ldprotest.db.codec.UUIDCodec;
import ldprotest.main.ServerTime;
import ldprotest.geo.Coordinate;
import ldprotest.serialization.BsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import org.bson.BsonDateTime;

public class PrivateProtestData implements BsonSerializable {

    private static final String COLLECTION_NAME = "protests";

    public final Coordinate location;
    public final Optional<String> owner;
    public final UUID ownerId;

    public final String title;
    public final String description;
    public final Optional<String> dressCode;
    public final Optional<String> homePage;

    public final Optional<ZonedDateTime> date;
    public final Optional<Integer> recursEveryDays;

    public final UUID protestId;

    @ReflectiveConstructor
    private PrivateProtestData() {
        location = null;
        owner = null;
        ownerId = null;

        title = null;
        description = null;
        dressCode = null;
        date = null;
        protestId = null;
        homePage = null;
        recursEveryDays = null;
    }

    private PrivateProtestData(
        Coordinate location,
        Optional<String> owner,
        UUID ownerId,
        String title,
        String description,
        Optional<ZonedDateTime> date,
        Optional<String> dressCode,
        UUID protestId,
        Optional<String> homePage,
        Optional<Integer> recursEveryDays
    ) {
        this.location = location;
        this.owner = owner;
        this.ownerId = ownerId;
        this.title = title;
        this.description = description;
        this.dressCode = dressCode;
        this.date = date;
        this.protestId = protestId;
        this.homePage = homePage;
        this.recursEveryDays = recursEveryDays;
    }

    public static PrivateProtestData generate(PublicProtestData data, UUID userId) {
        return new PrivateProtestData(
            data.location,
            Optional.of(data.owner.get()),
            userId,
            data.title,
            data.description,
            data.date,
            data.dressCode,
            UUID.randomUUID(),
            data.homePage,
            Optional.of(data.recursEveryDays)
        );
    }

    public static PrivateProtestData publicToPrivate(PublicProtestData data, UUID userId, UUID protestId) {
        return new PrivateProtestData(
            data.location,
            Optional.of(data.owner.get()),
            userId,
            data.title,
            data.description,
            data.date,
            data.dressCode,
            protestId,
            data.homePage,
            Optional.of(data.recursEveryDays)
        );
    }

    public static void setupDbIndex() {

        MongoCollection<PrivateProtestData> collection = collection();

        IndexTools.createIndexWithOpts(collection, Indexes.ascending("protestId"), true, false);
        IndexTools.createIndexWithOpts(collection, Indexes.ascending("date"), false, false);

        collection.createIndex(Indexes.geo2dsphere("location"));
    }

    public static MongoCollection<PrivateProtestData> collection() {
        return MainDatabase.database().getCollection(COLLECTION_NAME, PrivateProtestData.class);
    }

    public static PrivateProtestData lookupByProtestId(UUID protestId) {
        return collection().find(
            Filters.eq("protestId", UUIDCodec.toBsonValue((protestId)))
        ).first();
    }

    public static long deleteOlderThan(long ageMillis) {
        long now = ServerTime.nowMillis();
        BsonDateTime threshold = new BsonDateTime(now - ageMillis);

        return collection().deleteMany(
            Filters.and(
                Filters.lt("date", threshold),
                Filters.or(Filters.exists("recursEveryDays", false), Filters.lte("recursEveryDays", 0))
            )
        ).getDeletedCount();
    }

    public static boolean updateProtest(PrivateProtestData protest) {
        ReplaceOptions options = new ReplaceOptions();
        options.upsert(false);

        UpdateResult result = collection().replaceOne(
            Filters.eq("protestId", UUIDCodec.toBsonValue(protest.protestId)), protest, options
        );

        return result.getModifiedCount() == 1;
    }

    public static boolean deleteProtest(UUID protestId) {
        DeleteResult result = collection().deleteOne(Filters.eq("protestId", UUIDCodec.toBsonValue(protestId)));

        return result.getDeletedCount() == 1;
    }
}
