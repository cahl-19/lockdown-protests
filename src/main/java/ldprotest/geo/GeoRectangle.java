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
import com.mongodb.client.model.geojson.Polygon;
import com.mongodb.client.model.geojson.Position;
import java.util.Arrays;
import java.util.List;
import org.bson.conversions.Bson;

public class GeoRectangle {

    private final Coordinate swCorner;
    private final Coordinate neCorner;

    public GeoRectangle(Coordinate swCorner, Coordinate neCorner) {
        this.swCorner = swCorner;
        this.neCorner = neCorner;
    }

    @SuppressWarnings("unchecked")
    public Bson bsonFilter(String field) {

        Position northWest = new Position(swCorner.longitude, neCorner.latitude);
        Position northEast = new Position(neCorner.longitude, neCorner.latitude);
        Position southEast = new Position(neCorner.longitude, swCorner.latitude);
        Position southWest = new Position(swCorner.longitude, swCorner.latitude);

        List<Position> points = Arrays.asList(northWest, northEast, southEast, southWest, northWest);

        return Filters.geoWithin(field, new Polygon(points));
    }
}