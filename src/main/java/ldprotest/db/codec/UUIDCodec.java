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

import java.util.UUID;
import org.bson.BsonArray;
import org.bson.BsonInt64;
import org.bson.BsonReader;
import org.bson.BsonValue;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class UUIDCodec implements Codec<UUID>  {

    @Override
    public Class<UUID> getEncoderClass() {
        return UUID.class;
    }

    @Override
    public void encode(BsonWriter writer, UUID val, EncoderContext ctx) {
        encode(writer, val);
    }

    public static void encode(BsonWriter writer, UUID uuid) {
        writer.writeStartArray();

        writer.writeInt64(uuid.getMostSignificantBits());
        writer.writeInt64(uuid.getLeastSignificantBits());

        writer.writeEndArray();
    }

    @Override
    public UUID decode(BsonReader reader, DecoderContext ctx) {
        return decode(reader);
    }

    public static UUID decode(BsonReader reader) {
        reader.readStartArray();

        long msb = reader.readInt64();
        long lsb = reader.readInt64();

        reader.readEndArray();

        return new UUID(msb, lsb);
    }

    public static BsonValue toBsonValue(UUID value) {
        BsonArray ret = new BsonArray();

        ret.add(new BsonInt64(value.getMostSignificantBits()));
        ret.add(new BsonInt64(value.getLeastSignificantBits()));

        return ret;
    }
}
