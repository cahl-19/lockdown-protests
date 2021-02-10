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
import com.mongodb.client.model.Indexes;
import java.time.ZonedDateTime;
import java.util.Optional;
import ldprotest.db.MainDatabase;
import ldprotest.geo.Coordinate;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.serialization.Sanitizable;
import ldprotest.server.infra.http.Sanitize;
import ldprotest.util.interfaces.Validatable;

public class ProtestData implements JsonSerializable, Sanitizable<ProtestData>, Validatable {

    private static final String COLLECTION_NAME = "protests";

    private static final int MIN_TITLE_LENGTH = 4;
    private static final int MAX_TITLE_LENGTH = 256;
    private static final int MAX_DESCRIPTION_LENGTH = 768;
    private static final int MAX_DRESS_CODE_LENGTH = 128;


    public final Coordinate location;
    public final Optional<String> owner;

    public final String title;
    public final String description;
    public final Optional<String> dressCode;

    public final ZonedDateTime date;

    @ReflectiveConstructor
    private ProtestData() {
        location = null;
        owner = null;

        title = null;
        description = null;
        dressCode = null;
        date = null;
    }

    private ProtestData(
        Coordinate location,
        Optional<String> owner,
        String title,
        String description,
        ZonedDateTime date,
        Optional<String> dressCode
    ) {
        this.location = location;
        this.owner = owner;
        this.title = title;
        this.description = description;
        this.dressCode = dressCode;
        this.date = date;
    }

    public ProtestData(
        Coordinate location, String owner, String title, String description, ZonedDateTime date, String dressCode
    ) {
        this(location, Optional.of(owner), title, description, date, Optional.of(dressCode));
    }

    public ProtestData(Coordinate location, String owner, String title, String description, ZonedDateTime date) {
        this(location, Optional.of(owner), title, description, date, Optional.empty());
    }

    @Override
    public boolean validate() {
        if(title.length() > MAX_TITLE_LENGTH) {
            return false;
        } else if(title.length() < MIN_TITLE_LENGTH) {
            return false;
        } else if(description.length() > MAX_DESCRIPTION_LENGTH) {
            return false;
        } else if(dressCode.isPresent() && dressCode.get().length() > MAX_DRESS_CODE_LENGTH) {
            return false;
        } else if(!location.validate()) {
            return false;
        } else {
            return true;
        }
    }

    @Override
    public ProtestData sanitize() {
        return new ProtestData(
            location,
            owner,
            Sanitize.encodeHtml(title),
            Sanitize.encodeHtml(description),
            date,
            dressCode.isEmpty() ? dressCode : Optional.of(Sanitize.encodeHtml(dressCode.get()))
        );
    }

    public static void setupDbIndex() {
        MongoCollection<ProtestData> collection = collection();

        collection.createIndex(Indexes.geo2dsphere("location"));
    }

    public static MongoCollection<ProtestData> collection() {
        return MainDatabase.database().getCollection(COLLECTION_NAME, ProtestData.class);
    }
}
