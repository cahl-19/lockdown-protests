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
package ldprotest.db.codec;

import org.bson.BsonBinary;
import org.bson.BsonReader;
import org.bson.BsonSerializationException;
import org.bson.BsonType;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.types.ObjectId;
import ldprotest.serialization.BsonSerializable;
import ldprotest.util.ReflectionTools;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import ldprotest.geo.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ldprotest.util.PrintTools;

public class BsonSerializableCodec <T> implements Codec<T> {

    private static final Map<Type, FieldEncoder> FIELD_ENCODERS = initFieldEncoders();
    private static final Map<Type, TypeEncoder> TYPE_ENCODERS = initTypeEncoders();

    private static final Map<Type, FieldDecoder> FIELD_DECODERS = initFieldDecoders();
    private static final Map<Type, TypeDecoder> TYPE_DECODERS = initTypeDecoders();

    private final Class<T> clazz;
    private final Map<String, Field> fields;

    private final static String ID_FIELD_NAME = "_id";

    private final static Logger LOGGER = LoggerFactory.getLogger(BsonSerializableCodec.class);

    public BsonSerializableCodec(Class<T> clazz) {
        this.clazz = clazz;
        this.fields = collectFields(ReflectionTools.instanceFields(clazz));
    }

    @Override
    public T decode(BsonReader reader, DecoderContext decoderContext) {
        try {
            return decodeWithoutContext(reader, getEncoderClass(), fields);
        } catch (InvocationTargetException | NoSuchMethodException | InstantiationException e) {
            throw new BsonSerializationException(
                String.format("Unable to construct class %s for decoding: %s.",
                getEncoderClass().getName(), e.getMessage()
            ));
        } catch (IllegalAccessException e) {
            throw new BsonSerializationException(
                String.format("Unable to construct class %s: %s.",
                    getEncoderClass().getName(), e.getMessage()
                ));
        }
    }

    @Override
    public void encode(BsonWriter writer, T value, EncoderContext encoderContext) {
        try {
            encodeWithoutContext(writer, fields.values(), value);
        } catch (
            IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e
        ) {
            throw new BsonSerializationException(
                String.format(
                    "Illegal access exception during serialization on class %s: %s.",
                    clazz.getName(), e.getMessage()
                )
            );
        }
    }

    @Override
    public Class<T> getEncoderClass() {
        return clazz;
    }

    private static <T> T decodeWithoutContext(BsonReader reader, Class<T> clazz, Map<String, Field> fields)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        Set<Field> unsetFields = new HashSet<>(fields.values());

        T decoded = ReflectionTools.construct(clazz);

        reader.readStartDocument();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            String name = reader.readName();
            Field field = fields.get(name);

            if(field == null && name.equals(ID_FIELD_NAME)) {
                reader.readObjectId();
                continue;
            } else if(field == null) {
                throw new BsonSerializationException(String.format(
                    "Unexpected Field '%s' in deserialization of class %s",
                    name, clazz.getName()
                ));
            }

            ReflectionTools.fieldAccessibleContext(field, () -> {
                FieldDecoder fieldDecoder = FIELD_DECODERS.get(field.getType());
                if(fieldDecoder != null) {
                    fieldDecoder.decode(reader, field, decoded);
                } else {
                    field.set(decoded, decodeType(reader, field.getGenericType(), field.getType()));
                }
            });

            unsetFields.remove(field);
        }

        reader.readEndDocument();

        unsetFields.removeIf((f) -> Optional.class.isAssignableFrom(f.getType()));

        if(!unsetFields.isEmpty()) {
            throw new BsonSerializationException(
                "Required fields not set: %s" + PrintTools.listPrint(unsetFields)
            );
        }

        return decoded;
    }

    private static void encodeWithoutContext(BsonWriter writer, Collection<Field> fields, Object value)
        throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {

        writer.writeStartDocument();
        for (Field field : fields) {
            ReflectionTools.fieldAccessibleContext(field, () -> {
                FieldEncoder fieldEncoder = FIELD_ENCODERS.get(field.getType());
                if (fieldEncoder != null) {
                    fieldEncoder.encode(writer, field, value);
                } else {
                    writer.writeName(field.getName());
                    encodeType(writer, field.getGenericType(), field.getType(), field.get(value));
                }
            });
        }
        writer.writeEndDocument();
    }

    @SuppressWarnings("unchecked")
    private static void encodeType(
        BsonWriter writer, Type type, Class<?> clazz, Object object
    ) throws IllegalAccessException, NoSuchMethodException, InstantiationException, InvocationTargetException {

        if(encodeNonGenericType(writer, clazz, object)) {
            return;
        }
        ParameterizedType parmType = (ParameterizedType)type;

        if (Map.class.isAssignableFrom(clazz)) {
            encodeMap(parmType, clazz, (Map<String, Object>)object, writer);
         } else if(Collection.class.isAssignableFrom(clazz)) {
            encodeCollection(parmType, clazz, (Collection)object, writer);
        } else if (Optional.class.isAssignableFrom(clazz)) {
            encodeOptional(parmType, clazz, (Optional)object, writer);
        } else if(clazz.isEnum()) {
           writer.writeString(((Enum<?>)object).name());
        } else if (BsonSerializable.class.isAssignableFrom(clazz)) {
            encodeWithoutContext(writer, ReflectionTools.instanceFields(clazz), object);
        } else {
            throw new BsonSerializationException(String.format("Unable to serialize %s", clazz.getName()));
        }
    }

    private static boolean encodeNonGenericType(
            BsonWriter writer, Class<?> clazz, Object object
    ) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        TypeEncoder typeEncoder = TYPE_ENCODERS.get(clazz);
         if(typeEncoder != null) {
            typeEncoder.encode(writer, object);
            return true;
        } else if(clazz.isEnum()) {
           writer.writeString(((Enum<?>)object).name());
           return true;
        } else if (BsonSerializable.class.isAssignableFrom(clazz)) {
            encodeWithoutContext(writer, ReflectionTools.instanceFields(clazz), object);
            return true;
        }

        return false;
    }

    private static void encodeOptional(
        ParameterizedType type, Class<?> clazz, Optional<?> optional, BsonWriter writer
    ) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {
        List<Type> typeParameters = ReflectionTools.getGenericTypeParameters(type);
        Type t = typeParameters.get(0);

        assert(typeParameters.size() == 1);

        if(optional.isEmpty()) {
            return;
        }

        encodeNonGenericType(writer, (Class<?>)t, optional.get());
    }


    private static void encodeMap(
        ParameterizedType type, Class<?> clazz, Map<String, Object> map, BsonWriter writer
    ) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {

        List<Type> typeParameters = ReflectionTools.getGenericTypeParameters(type);
        Type t = typeParameters.get(1);

        assert(typeParameters.size() == 2);

        TypeEncoder typeEncoder = TYPE_ENCODERS.get(t);

        if(!typeParameters.get(0).equals(String.class)) {
            throw new BsonSerializationException("Unable to serialize map with non-string key.");
        }

        writer.writeStartDocument();

        if(typeEncoder != null) {
            for (Entry<String, Object> entry : map.entrySet()) {
                writer.writeName(entry.getKey());
                typeEncoder.encode(writer, entry.getValue());
            }
        } else {
            for (Entry<String, Object> entry : map.entrySet()) {
                writer.writeName(entry.getKey());
                encodeNonGenericType(writer, (Class<?>) t, entry.getValue());
            }
        }

        writer.writeEndDocument();
    }

    private static void encodeCollection(
        ParameterizedType type, Class<?> clazz, Collection<?> collection, BsonWriter writer
    ) throws IllegalAccessException, NoSuchMethodException, InvocationTargetException, InstantiationException {

        List<Type> typeParameters = ReflectionTools.getGenericTypeParameters(type);
        Type t = typeParameters.get(0);

        assert(typeParameters.size() == 1);

        TypeEncoder typeEncoder = TYPE_ENCODERS.get(clazz);

        writer.writeStartArray();

        if(typeEncoder != null) {
            for (Object o : collection) {
                typeEncoder.encode(writer, o);
            }
        } else {
            for (Object o : collection) {
                encodeNonGenericType(writer, (Class<?>) t, o);
            }
        }

        writer.writeEndArray();
    }

    private static Map<Type, FieldEncoder> initFieldEncoders() {
        Map<Type, FieldEncoder> map = new HashMap<>();

        map.put(Byte.TYPE, (writer, field, object) -> writer.writeInt32(field.getName(), field.getByte(object)));
        map.put(Character.TYPE, (writer, field, object) -> writer.writeInt32(field.getName(), field.getChar(object)));

        map.put(Short.TYPE, (writer, field, object) -> writer.writeInt32(field.getName(), field.getShort(object)));
        map.put(Integer.TYPE, (writer, field, object) -> writer.writeInt32(field.getName(), field.getInt(object)));
        map.put(Long.TYPE, (writer, field, object) -> writer.writeInt64(field.getName(), field.getLong(object)));

        map.put(Float.TYPE, (writer, field, object) -> writer.writeDouble(field.getName(), field.getFloat(object)));
        map.put(Double.TYPE, (writer, field, object) -> writer.writeDouble(field.getName(), field.getInt(object)));

        map.put(
            Boolean.TYPE, (writer, field, object) -> writer.writeBoolean(field.getName(), field.getBoolean(object))
        );

        return map;
    }

    private static Map<Type, TypeEncoder> initTypeEncoders() {
        Map<Type, TypeEncoder> map = new HashMap<>();

        map.put(Byte.class, (writer, object) -> writer.writeInt32((Byte)object));
        map.put(Character.class, (writer, object) -> writer.writeInt32((Character)object));

        map.put(Short.class, (writer, object) -> writer.writeInt32((Short)object));
        map.put(Integer.class, (writer, object) -> writer.writeInt32((Integer)object));
        map.put(Long.class, (writer, object) -> writer.writeInt64((Long)object));

        map.put(Float.class, (writer, object) -> writer.writeDouble((Float)object));
        map.put(Double.class, (writer, object) -> writer.writeDouble((Double)object));

        map.put(String.class, (writer, object) -> writer.writeString((String)object));

        map.put(ObjectId.class, (writer, object) -> writer.writeObjectId((ObjectId)object));

        map.put(byte[].class, (writer, object) -> {
            byte[] data = (byte[])object;
            writer.writeBinaryData(new BsonBinary(data));
        });

        map.put(ZonedDateTime.class, (writer, object) -> {
            ZonedDateTime dt = (ZonedDateTime)object;
            long millisSinceEpoch = 1000 * dt.toEpochSecond() + dt.getNano() / 1000;
            writer.writeDateTime(millisSinceEpoch);
        });

        map.put(Coordinate.class, (writer, object) -> {
            CoordinateCodec.encode(writer, (Coordinate)object);
        });

        return map;
    }

    private static Map<?, ?> decodeMap(
        BsonReader reader, ParameterizedType type, Class<?> clazz
    ) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {

        Map<String, Object> map = new HashMap<>();
        List<Type> typeParameters = ReflectionTools.getGenericTypeParameters(type);

        if(!typeParameters.get(0).equals(String.class)) {
            throw new BsonSerializationException("Unable to deserialize map with non-string key.");
        }

        reader.readStartDocument();

        String key;
        while((key = reader.readName()) != null) {
            map.put(key, decodeNonGenericType(reader, (Class<?>)typeParameters.get(1)).get());
        }

        reader.readEndDocument();

        return map;
    }

    private static List<?> decodeList(BsonReader reader, ParameterizedType type, Class<?> clazz)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        List<Object> list = new ArrayList<>();
        Type listType = ReflectionTools.getGenericTypeParameters(type).get(0);

        reader.readStartArray();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            list.add(decodeNonGenericType(reader, (Class<?>)listType).get());
        }

        reader.readEndArray();

        return list;
    }

    private static Set<?> decodeSet(
        BsonReader reader, ParameterizedType type, Class<?> clazz
    ) throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        Set<Object> set = new HashSet<>();
        Type setType = ReflectionTools.getGenericTypeParameters(type).get(0);

        reader.readStartArray();

        while(reader.readBsonType() != BsonType.END_OF_DOCUMENT) {
            set.add(decodeNonGenericType(reader, (Class<?>)setType).get());
        }

        reader.readEndArray();
        return set;
    }

    private static Optional<?> decodeOptional(BsonReader reader, ParameterizedType type, Class<?> clazz)
        throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException
    {
        Type optType = ReflectionTools.getGenericTypeParameters(type).get(0);

        if(reader.getCurrentBsonType() == BsonType.NULL) {
            return Optional.empty();
        } else {
            return Optional.of(decodeNonGenericType(reader, (Class<?>)optType).get());
        }
    }

    private static Object decodeEnum(BsonReader reader, Class<?> clazz) {
        Enum<?>[] constants = (Enum<?>[])clazz.getEnumConstants();
        String e = reader.readString();

        for(int i = 0; i < constants.length; i++) {
            if(constants[i].name().equals(e)) {
                return constants[i];
            }
        }
        throw new BsonSerializationException(String.format("Unable to deserialize enum %s.", clazz.getName()));
    }

    @SuppressWarnings("unchecked")
    private static Object decodeType(
        BsonReader reader, Type type, Class<?> clazz
    ) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        Optional nonGenericObject = decodeNonGenericType(reader, clazz);

        if(nonGenericObject.isPresent()) {
            return nonGenericObject.get();
        } else if(clazz.isEnum()) {
            return decodeEnum(reader, clazz);
        }

        ParameterizedType paramType = (ParameterizedType)type;

        if (Map.class.isAssignableFrom(clazz)) {
            return decodeMap(reader, paramType, clazz);
        } else if(List.class.isAssignableFrom(clazz)) {
            return decodeList(reader, paramType, clazz);
        } else if(Set.class.isAssignableFrom(clazz)) {
            return decodeSet(reader, paramType, clazz);
        } else if (Optional.class.isAssignableFrom(clazz)) {
            return decodeOptional(reader, paramType, clazz);
        } else {
            throw new BsonSerializationException(String.format("Unable to deserialize %s.", clazz.getName()));
        }
    }

    @SuppressWarnings("unchecked")
    private static Optional<Object> decodeNonGenericType(
        BsonReader reader, Class<?> clazz
    ) throws NoSuchMethodException, InstantiationException, IllegalAccessException, InvocationTargetException {

        TypeDecoder decoder = TYPE_DECODERS.get(clazz);

        if(decoder != null) {
            return Optional.of(decoder.decode(reader, clazz));
        } else if (BsonSerializable.class.isAssignableFrom(clazz)) {
            return Optional.of(
                decodeWithoutContext(reader, clazz, collectFields(ReflectionTools.instanceFields(clazz)))
            );
        } else {
            return Optional.empty();
        }
    }

    private static Map<Type, FieldDecoder> initFieldDecoders() {
        Map<Type, FieldDecoder> map = new HashMap<>();

        map.put(
            Byte.TYPE, (reader, field, object) -> field.setByte(object, (byte)reader.readInt32())
        );
        map.put(
            Character.TYPE, (reader, field, object) -> field.setChar(object, (char)reader.readInt32())
        );

        map.put(
            Short.TYPE, (reader, field, object) -> {
                field.setShort(field.getName(), (short)reader.readInt32());
            }
        );
        map.put(
            Integer.TYPE, (reader, field, object) -> field.setInt(object, reader.readInt32())
        );
        map.put(
            Long.TYPE, (reader, field, object) -> field.setLong(object, reader.readInt64())
        );
        map.put(
            Float.TYPE, (reader, field, object) -> {
                field.setFloat(field.getName(), (float)reader.readDouble());
            }
        );
        map.put(
            Double.TYPE, (reader, field, object) -> {
                field.setDouble(object, (short)reader.readInt32());
            }
        );
        map.put(
            Boolean.TYPE, (reader, field, object) -> {
                field.setBoolean(object, reader.readBoolean());
            }
        );

        return map;
    }

    private static Map<Type, TypeDecoder> initTypeDecoders() {
        Map<Type, TypeDecoder> map = new HashMap<>();

        map.put(Byte.class, (reader, clazz) -> (byte)reader.readInt32());
        map.put(Character.class, (reader, clazz) -> (char)reader.readInt32());

        map.put(Short.class, (reader, clazz) -> (short)reader.readInt32());
        map.put(Integer.class, (reader, clazz) -> reader.readInt32());
        map.put(Long.class, (reader, clazz) -> reader.readInt64());

        map.put(Float.class, (reader, clazz) -> (float)reader.readDouble());
        map.put(Double.class, (reader, clazz) -> reader.readDouble());

        map.put(String.class, (reader, clazz) -> reader.readString());

        map.put(ObjectId.class, (reader, clazz) -> reader.readObjectId());

        map.put(byte[].class, (reader, clazz) -> reader.readBinaryData().getData());

        map.put(ZonedDateTime.class, (reader, clazz) -> {
            long millisSinceEpoch = reader.readDateTime();
            return ZonedDateTime.ofInstant(Instant.ofEpochMilli(millisSinceEpoch), ZoneOffset.UTC);
        });

        map.put(Coordinate.class, (reader, clazz) -> {
            return CoordinateCodec.decode(reader);
        });

        return map;
    }

    private static Map<String, Field> collectFields(Collection<Field> fields) {
        Map<String, Field> fieldMap = new HashMap<>(fields.size());
        for(Field f: fields) {
            fieldMap.put(f.getName(), f);
        }
        return fieldMap;
    }

    @FunctionalInterface
    private interface FieldEncoder {
        void encode(BsonWriter writer, Field field, Object object) throws IllegalAccessException;
    }

    @FunctionalInterface
    private interface TypeEncoder {
        void encode(BsonWriter writer, Object object);
    }

    @FunctionalInterface
    private interface FieldDecoder {
        void decode(BsonReader reader, Field field, Object object) throws IllegalAccessException;
    }

    @FunctionalInterface
    private interface TypeDecoder<T> {
        T decode(BsonReader reader, Class<T> clazz);
    }
}
