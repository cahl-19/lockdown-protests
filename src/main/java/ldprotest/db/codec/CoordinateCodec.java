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

import ldprotest.geo.Coordinate;
import org.bson.BsonReader;
import org.bson.BsonWriter;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;

public class CoordinateCodec implements Codec<Coordinate>  {

    @Override
    public Class<Coordinate> getEncoderClass() {
        return Coordinate.class;
    }

    @Override
    public void encode(BsonWriter writer, Coordinate coord, EncoderContext ctx) {
        encode(writer, coord);
    }

    public static void encode(BsonWriter writer, Coordinate coord) {
        writer.writeStartDocument();

        writer.writeString("type", "Point");

        writer.writeStartArray("coordinates");

        writer.writeDouble(coord.longitude);
        writer.writeDouble(coord.latitude);

        writer.writeEndArray();

        writer.writeEndDocument();
    }

    @Override
    public Coordinate decode(BsonReader reader, DecoderContext ctx) {
        return decode(reader);
    }

    public static Coordinate decode(BsonReader reader) {
        reader.readStartDocument();

        reader.readString("type");

        reader.readName("coordinates");

        reader.readStartArray();

        double longitude = reader.readDouble();
        double latitude = reader.readDouble();

        reader.readEndArray();

        reader.readEndDocument();

        return new Coordinate(latitude, longitude);
    }
}
