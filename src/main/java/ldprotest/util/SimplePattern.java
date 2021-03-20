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

import ldprotest.util.types.MayFail;

public final class SimplePattern {
    private SimplePattern() {
        /* do not construct */
    }

    public static String replace(String pattern, String keyStr, String replace) {
        int i = pattern.indexOf(keyStr);

        if(i < 0) {
            return pattern;
        } else {
            return pattern.substring(0, i) + replace + pattern.substring(i + keyStr.length());
        }
    }

    public static String replace(String pattern, String keyStr, int replace) {
        return replace(pattern, keyStr, Integer.toString(replace));
    }

    public static MayFail<String> match(String str, String pattern, String keyString) {
        int i = pattern.indexOf(keyString);

        int split0 = i > 0 ? i : pattern.length();
        int split1 = i > 0 ? i + keyString.length() : pattern.length();

        String prefix = pattern.substring(0, split0);
        String suffix = pattern.substring(split1);

        if(str.startsWith(prefix) && str.endsWith(suffix)) {
            return MayFail.success(str.substring(prefix.length(), str.length() - suffix.length()));
        } else {
            return MayFail.failure();
        }
    }

    public static MayFail<Integer> matchInt(String str, String pattern, String keyString) {
        MayFail<String> sMatch = match(str, pattern, keyString);

        if(sMatch.isFailure()) {
            return MayFail.failure();
        }

        try {
            return MayFail.success(Integer.parseInt(sMatch.result()));
        } catch(NumberFormatException e) {
            return MayFail.failure();
        }
    }
}
