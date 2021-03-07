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
package ldprotest.geo;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.geojson.NamedCoordinateReferenceSystem;
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.PolygonCoordinates;
import com.mongodb.client.model.geojson.Position;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.bson.BsonArray;
import org.bson.BsonDouble;
import org.bson.conversions.Bson;

public class GeoRectangle {

    private static final String CRS = "urn:x-mongodb:crs:strictwinding:EPSG:4326";

    private final Coordinate swCorner;
    private final Coordinate neCorner;

    public GeoRectangle(Coordinate swCorner, Coordinate neCorner) {
        this.swCorner = swCorner;
        this.neCorner = neCorner;
    }

    @SuppressWarnings("unchecked")
    public Bson bsonFilter(String field) {

        List<Position> vertices = new ArrayList<>(8);

        double north = neCorner.latitude;
        double east = normalize_longitude(neCorner.longitude);
        double south = swCorner.latitude;
        double west = normalize_longitude(swCorner.longitude);

        vertices.add(new Position(east, north));

        for(double i = east; circularCompare(west, i) > 45.0;  i = circularAdd(i, -45.0)) {
            vertices.add(new Position(i, north));
        }

        vertices.add(new Position(west, north));
        vertices.add(new Position(west, south));

        for(double i = west; circularCompare(i, east) > 45.0;  i = circularAdd(i, -45.0)) {
            vertices.add(new Position(i, south));
        }

        vertices.add(new Position(east, south));
        vertices.add(new Position(east, north));

        Polygon poly = new Polygon(
            NamedCoordinateReferenceSystem.EPSG_4326_STRICT_WINDING,
            new PolygonCoordinates​(vertices)
        );

        return Filters.geoWithin(field, poly);
    }

    private static BsonArray coordinate(double longitude, double lattidue) {
        BsonArray ret = new BsonArray();

        ret.add(new BsonDouble(longitude));
        ret.add(new BsonDouble(lattidue));

        return ret;
    }

    private static double circularAdd(double x, double inc) {
        return normalize_longitude(x + inc);
    }

    private static double circularCompare(double west, double east) {
        if(west < east) {
            return east - west;
        } else {
            return (east - west) + 360.0;
        }
    }

    private static double normalize_longitude(double lng) {
        if(lng > 180.0) {
            lng += 180;

            lng -= Math.floor(lng / 360.0) * 360.0;

            lng -= 180;
        } else if(lng < -180.0) {
            lng -= 180.0;

            lng += Math.floor(-lng / 360.0) * 360.0;

            lng += 180;
        }

        return lng;
    }
}
