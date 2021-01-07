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

import org.bson.codecs.Codec;
import org.bson.codecs.configuration.CodecRegistry;
import ldprotest.serialization.BsonSerializable;
import ldprotest.util.CountLimitedLruCache;

public class CodecProvider implements org.bson.codecs.configuration.CodecProvider {

    private static final int MAX_CACHED_CODECS = 32;

    private final CountLimitedLruCache<Class<?>, Codec<?>> codecCache;

    public CodecProvider() {
        codecCache = new CountLimitedLruCache<>(MAX_CACHED_CODECS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> Codec<T> get(Class<T> clazz, CodecRegistry registry) {

        if(BsonSerializable.class.isAssignableFrom(clazz)) {
            return (Codec<T>) codecCache.computeIfAbsent(clazz, () -> new BsonSerializableCodec(clazz));
        } else {
            return null;
        }
    }
}
