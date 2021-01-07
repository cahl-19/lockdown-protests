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

import java.io.File;
import java.util.Arrays;
import java.util.List;


public class PathUtils {

    private PathUtils() {
        /* do not construct */
    }

    public static List<String> split(String path) {
        return Arrays.asList(path.split(File.separator));
    }

    public static String basename(String path) {
        List<String> elements = split(path);

        if(elements.isEmpty()) {
            return "";
        } else {
            return elements.get(elements.size() - 1);
        }
    }

    public static String extension(String path) {
        String filename = basename(path);
        String [] elements = path.split("\\.");

        if(elements.length == 0 || elements.length == 1) {
            return "";
        } else {
            List<String> extensionParts = Arrays.asList(Arrays.copyOfRange(elements, 1, elements.length));
            return String.join(".", extensionParts);
        }
    }

    public static String stripExtension(String path) {
        String filename = basename(path);
        String [] elements = filename.split("\\.");

        if(elements.length == 0) {
            return "";
        } else {
            return elements[0];
        }
    }
}
