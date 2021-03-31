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
package ldprotest.util.types;

import java.util.Arrays;

public class Ipv4Address {

    public final String stringRep;

    private final int octets[];
    private final long numeric;

    public Ipv4Address(String value) {
        stringRep = value;
        octets = parse(value);
        numeric = numericValue(octets);
    }

    public long numeric() {
        return numeric;
    }

    public int[] octets() {
        return Arrays.copyOf(octets, octets.length);
    }

    private static int[] parse(String value) {
        String[] parts = value.split("\\.");
        int ret[] = new int[4];

        if(parts.length != 4) {
            throw new IllegalArgumentException("Invalid IP address format: " + value);
        }

        for(int i = 0; i < 4; i++) {
            ret[i] = Integer.parseInt(parts[i]);
            if(ret[i] < 0 || ret[i] > 255) {
                throw new IllegalArgumentException("Invalid octet value in input " + value + ", " + parts[i]);
            }
        }
        return ret;
    }

    private static long numericValue(int octets[]) {
        int shift = 0;
        long result = 0;

        for(int i = 3; i >=0; i--) {
            result += ((long)octets[i]) << shift;
            shift += 8;
        }

        return result;
    }
}
