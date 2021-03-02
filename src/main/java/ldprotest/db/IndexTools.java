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

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ldprotest.serialization.BsonSerializable;
import org.bson.conversions.Bson;

public final class IndexTools {

    private IndexTools() {
        /* do not construct */
    }

    public static void createIndexWithOpts(MongoCollection<?> collection, Bson index, boolean unique, boolean sparse) {
        IndexOptions options = new IndexOptions();

        options.unique(unique);
        options.sparse(sparse);

        collection.createIndex(index, options);
    }

    public static Collection<Bson> reflectiveBuildIndexes(Class<?> clazz) {
        List<Bson> ret = new ArrayList<>();
        Map<Integer, List<Bson>> indexFields = new HashMap<>();
        recursiveFindIndexParams(clazz, "", indexFields);

        for(List<Bson> list: indexFields.values()) {
            if(list.size() == 1) {
                ret.add(list.get(0));
            } else {
                ret.add(Indexes.compoundIndex(list));
            }
        }

        return ret;
    }

    private static void recursiveFindIndexParams(
        Class<?> clazz, String fieldPrefix, Map<Integer, List<Bson>> fields
    ) {
        for(Field f: clazz.getDeclaredFields()) {

            DbIndex annotation = f.getAnnotation(DbIndex.class);
            if(annotation != null) {
                    fields.compute(annotation.groupId(), (k, v) -> {
                        List<Bson> list;
                        if(v == null) {
                            list = new ArrayList<>();
                        } else {
                            list = v;
                        }
                        list.add(indexOf(annotation, fieldPrefix + f.getName()));
                        return list;
                    });
            } else if(BsonSerializable.class.isAssignableFrom(f.getType())) {
                recursiveFindIndexParams(f.getType(), f.getName() + ".", fields);
            }
        }
    }

    private static Bson indexOf(DbIndex ind, String name) {
        if(ind.order().equals(DbSortOrder.ASCENDING)) {
            return Indexes.ascending(name);
        } else {
            return Indexes.descending(name);
        }
    }
}
