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
package ldprotest.db;

import com.mongodb.MongoCommandException;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import ldprotest.business.PrivateProtestData;
import ldprotest.geo.geoip.GeoIpLookup;
import ldprotest.serialization.BsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.server.auth.UserAccount;
import ldprotest.server.auth.UserSessions;
import ldprotest.server.auth.AuthFailureLockout;

public final class SetupDatabase {

    public static final int CURRENT_MAJOR_VERSION = 0;
    public static final int CURRENT_MINOR_VERSION = 1;

    private static final String DB_VERSION_INFO_COLLECTION = "LDPROTEST_DB_VERSION";

    private static final int MONGO_NAMESPACE_EXISTS_ERROR_CODE = 48;

    private SetupDatabase() {
        /* do not construct */
    }

    public static DbVersionInfo setup(MongoDatabase db) {

        createCollections(db);

        MongoCollection<DbVersionInfo> collection = db.getCollection(DB_VERSION_INFO_COLLECTION, DbVersionInfo.class);

        setupIndexes();

        for(DbVersionInfo versionInfo: collection.find()) {
            return versionInfo;
        }

        DbVersionInfo info = new DbVersionInfo(CURRENT_MAJOR_VERSION, CURRENT_MINOR_VERSION);
        collection.insertOne(info);
        return info;
    }

    private static void createCollections(MongoDatabase db) {
        createCollectionIfNotExists(db, DB_VERSION_INFO_COLLECTION);
    }

    private static void setupIndexes() {
        UserAccount.setupDbIndex();
        UserSessions.setupDbIndex();
        PrivateProtestData.setupDbIndex();
        GeoIpLookup.setupDbIndex();
        AuthFailureLockout.setupDbIndex();
    }

    private static void createCollectionIfNotExists(MongoDatabase db, String collectionName) {
        try {
            db.createCollection(collectionName);
        } catch(MongoCommandException ex) {
            if(ex.getErrorCode() != MONGO_NAMESPACE_EXISTS_ERROR_CODE) {
                throw ex;
            }
        }
    }

    public static final class DbVersionInfo implements BsonSerializable {

        public final int majorVersion;
        public final int minorVersion;

        @ReflectiveConstructor
        private DbVersionInfo() {
            majorVersion = 0;
            minorVersion = 0;
        }

        private DbVersionInfo(int majorVersion, int minorVersion) {
            this.minorVersion = minorVersion;
            this.majorVersion = majorVersion;
        }

        public String toString() {
            return "DbVersion=" + majorVersion + "." + minorVersion;
        }
    }
}
