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
package ldprotest.util;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class PrintTools {

    private PrintTools() {
        /** Do not construct **/
    }

    @SuppressWarnings("unchecked")
    public static <T> String listPrint(T ...t) {
        return listPrint(Arrays.asList(t));
    }

    public static String listPrint(Iterable<?> i) {
        StringBuilder sb = new StringBuilder();

        sb.append('[');
        sb.append(String.join(", ", IterTools.mapIterable(i, (e) -> e.toString())));
        sb.append(']');

        return sb.toString();
    }

    public static String listPrint(Stream<?> s) {
        return listPrint(s.map((i) -> i.toString()).collect(Collectors.toList()));
    }

}
