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

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.MongoSocketOpenException;
import com.mongodb.MongoTimeoutException;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import ldprotest.db.codec.CodecProvider;
import ldprotest.main.Main;
import ldprotest.util.ErrorCode;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

import static org.bson.codecs.configuration.CodecRegistries.fromProviders;
import static org.bson.codecs.configuration.CodecRegistries.fromRegistries;

public final class MainDatabase {

    public static int MAIN_DATABASE_SHUTDOWN_HANDLER_PRIORITY = Main.SERVER_SHUTDOWN_HANDLER_PRIORITY + 8;

    private static MainDatabase SINGLETON_INSTANCE;

    private static final String MAIN_DATABASE_NAME = "ldprotest";

    private final MongoClient client;
    private final MongoDatabase database;

    private MainDatabase(MongoClient client) {
        CodecRegistry codecRegistry = fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            fromProviders(
                new CodecProvider(), PojoCodecProvider.builder().automatic(true).build()
            )
        );

        this.client = client;
        database = client.getDatabase(MAIN_DATABASE_NAME).withCodecRegistry(codecRegistry);
    }

    public static void connect(String connectString) {
        assignSingleton(() -> MongoClients.create(connectString));
    }

    public static void connect(ServerAddress ... addrs) {
        connect(Arrays.asList(addrs));
    }

    public static void connect(List<ServerAddress> addrs) {
        assignSingleton(() -> MongoClients.create(
            MongoClientSettings.builder()
                .applyToClusterSettings(
                    builder -> builder.hosts(addrs)
                ).build()
        ));
    }

    public static MongoClient client() {
        return SINGLETON_INSTANCE.client;
    }

    public static MongoDatabase database() {
        return SINGLETON_INSTANCE.database;
    }

    public static ErrorCode<MongoException> testConnection() {
        try {
            client().listDatabases().first();
            return ErrorCode.success();
        } catch (MongoTimeoutException | MongoSocketOpenException e) {
            return ErrorCode.error(e);
        } catch(MongoException e) {
            return ErrorCode.success();
        }
    }

    private static void assignSingleton(Supplier<MongoClient> supplier) {
        if(SINGLETON_INSTANCE != null) {
            throw new AssertionError("Main mongo client may only be setup once");
        }

        SINGLETON_INSTANCE = new MainDatabase(supplier.get());
        Main.registerShutdownHandler(MAIN_DATABASE_SHUTDOWN_HANDLER_PRIORITY, () -> client().close());
    }
}
