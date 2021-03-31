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
package ldprotest.geo.geoip;

import com.mongodb.MongoException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import java.util.List;
import ldprotest.db.DbIndex;
import ldprotest.db.DbSortOrder;
import ldprotest.db.IndexTools;
import ldprotest.db.MainDatabase;
import ldprotest.geo.Coordinate;
import ldprotest.serialization.BsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.util.Result;
import ldprotest.util.types.Ipv4Address;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GeoIpLookup {

    private static final double DEFAULT_LATITUDE  = 51.505;
    private static final double DEFAULT_LONGITUDE = -0.09;

    private static final Coordinate DEFAULT_COORDINATE = new Coordinate(DEFAULT_LATITUDE, DEFAULT_LONGITUDE);

    private final static Logger LOGGER = LoggerFactory.getLogger(GeoIpLookup.class);

    private static final String GEO_IP_TABLE_COLLECTION_NAME = "geoIp";

    private GeoIpLookup() {
        /* do not construct */
    }

    public static Result<GeoIpLookupError, Coordinate> lookup(String ipAddress) {
        return lookup(new Ipv4Address(ipAddress).numeric());
    }

    public static Result<GeoIpLookupError, Coordinate> lookup(long ipAddress) {

        try {
            GeoIpTableRow row = collection()
                .find(
                    Filters.and(
                        Filters.lte("ipLow", ipAddress),
                        Filters.gte("ipHigh", ipAddress)
                    )
                )
                .first();
            if(row == null) {
                return Result.failure(GeoIpLookupError.NOT_FOUND);
            } else {
                if(row.latitude == 0.0 && row.longitude == 0.0) {
                    return Result.success(DEFAULT_COORDINATE);
                } else {
                    return Result.success(new Coordinate(row.latitude, row.longitude));
                }
            }
        } catch(MongoException ex) {
            LOGGER.warn("Database error searcing for geo ip");
            return Result.failure(GeoIpLookupError.DB_ERROR);
        }
    }

    public static void write(List<GeoIpTableRow> rows) {
        collection().insertMany(rows);
    }

    public static void write(long ipLow, long ipHigh, double latitude, double longitude) {
        GeoIpTableRow row = new GeoIpTableRow(ipLow, ipHigh, latitude, longitude);
        collection().insertOne(row);
    }

    public static void drop() {
        collection().drop();
    }

    public static void setupDbIndex() {
        IndexOptions options = new IndexOptions();
        MongoCollection<GeoIpTableRow> collection = collection();

        options.unique(true);

        for(Bson index: IndexTools.reflectiveBuildIndexes(GeoIpTableRow.class)) {
            collection.createIndex(index, options);
        }
    }

    private static MongoCollection<GeoIpTableRow> collection() {
        return MainDatabase.database().getCollection(GEO_IP_TABLE_COLLECTION_NAME, GeoIpTableRow.class);
    }

    public enum GeoIpLookupError {
        NOT_FOUND,
        DB_ERROR;
    }

    public static final class GeoIpTableRow implements BsonSerializable {

        @DbIndex(order = DbSortOrder.ASCENDING, groupId = 0)
        public final long ipLow;
        @DbIndex(order = DbSortOrder.ASCENDING, groupId = 1)
        public final long ipHigh;

        public final double longitude;
        public final double latitude;

        @ReflectiveConstructor
        private GeoIpTableRow() {
            ipLow = -1;
            ipHigh = -1;
            longitude = -1.0;
            latitude = -1.0;
        }

        public GeoIpTableRow(long ipLow, long ipHigh, double latitude, double longitude) {
            this.ipLow = ipLow;
            this.ipHigh = ipHigh;
            this.longitude = longitude;
            this.latitude = latitude;
        }
    }
}
