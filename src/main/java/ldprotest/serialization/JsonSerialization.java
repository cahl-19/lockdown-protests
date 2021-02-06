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
package ldprotest.serialization;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.InstanceCreator;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Optional;
import ldprotest.util.ReflectionTools;

public class JsonSerialization {

    public static final Gson GSON = buildGson();

    private static Gson buildGson() {
        GsonBuilder builder = new GsonBuilder();

        builder.registerTypeHierarchyAdapter(JsonSerializable.class, new JsonSerializableSerializer());
        builder.registerTypeAdapter(Optional.class, new OptionalSerializer());
        builder.registerTypeAdapter(ZonedDateTime.class, new ZonedDateTimeSerializer());

        return builder.create();
    }

    private static final class ZonedDateTimeSerializer
        implements JsonSerializer<ZonedDateTime>, JsonDeserializer<ZonedDateTime> {

        @Override
        public JsonElement serialize(ZonedDateTime dt, Type type, JsonSerializationContext jsc) {
            return jsc.serialize(1000 * dt.toEpochSecond() + dt.getNano() / 1000);
        }

        @Override
        public ZonedDateTime deserialize(
            JsonElement je, Type type, JsonDeserializationContext jdc
        ) throws JsonParseException {
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(je.getAsInt()), ZoneOffset.UTC);
        }
    }

    private static final class OptionalSerializer<T>
        implements JsonSerializer<Optional<T>>, JsonDeserializer<Optional<T>> {

        @Override
        @SuppressWarnings("unchecked")
        public JsonElement serialize(Optional<T> t, Type type, JsonSerializationContext jsc) {
            if(t.isEmpty()) {
                return jsc.serialize(null);
            } else {
                T obj = t.get();
                return jsc.serialize(obj, obj.getClass());
            }
        }

        @Override
        public Optional<T> deserialize(
            JsonElement je, Type type, JsonDeserializationContext jdc
        ) throws JsonParseException {

            if(je.isJsonNull()) {
                return Optional.empty();
            } else {
                return Optional.of(
                    jdc.deserialize(je, ReflectionTools.getGenericTypeParameters((ParameterizedType)type).get(0))
                );
            }
        }
    }

    private static final class JsonSerializableSerializer<T>
        implements InstanceCreator<T>, JsonDeserializer<T> {

        @Override
        @SuppressWarnings("unchecked")
        public T createInstance(Type type) {
            try {
                return ReflectionTools.construct((Class<T>)type);
            } catch (
                NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException ex
            ) {
                throw new JsonParseException("Unable to construct class", ex);
            }
        }

        @Override
        @SuppressWarnings("unchecked")
        public T deserialize(JsonElement je, Type type, JsonDeserializationContext jdc) throws JsonParseException {

            Class<T> clazz = (Class<T>)type;
            T instance = createInstance(type);
            JsonObject jo = je.getAsJsonObject();

            for(Field f: ReflectionTools.instanceFields(clazz)) {

                JsonElement member = jo.get(f.getName());

                if(member == null && !Optional.class.isAssignableFrom(f.getType())) {
                    throw new JsonParseException(String.format(
                        "Missing required field %s in deserialization to class %s",
                        f.getName(), clazz.getCanonicalName()
                    ));
                }

                try {
                    ReflectionTools.fieldAccessibleContext(f, () -> {

                        if(member == null && Optional.class.isAssignableFrom(f.getType())) {
                            f.set(instance, Optional.empty());
                        } else {
                            f.set(instance, jdc.deserialize(member, f.getGenericType()));
                        }
                    });
                } catch(
                    IllegalAccessException | NoSuchMethodException | InstantiationException | InvocationTargetException
                    ex
                ) {
                    throw new JsonParseException(String.format(
                        "Unable to access field %s of class %s", f.getName(), clazz.getCanonicalName()
                    ));
                }
            }

            if(Sanitizable.class.isAssignableFrom(clazz)) {
                return ((Sanitizable<T>)instance).sanitize();
            } else {
                return instance;
            }
        }
    }
}
