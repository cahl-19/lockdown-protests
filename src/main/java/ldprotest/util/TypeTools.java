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


public class TypeTools {
    private TypeTools() {
        /* do not construct */
    }

    public static <T> T assertingCast(Object o, Class<T> type) {
        if(o == null || type.isAssignableFrom(o.getClass())) {
            return type.cast(o);
        } else {
            throw new AssertionError(String.format("Class %s expected assignable to type '%s'.", o.getClass(), type));
        }
    }

    public static <T> T assertingCast(Object o) {
        try {
            @SuppressWarnings("unchecked")
            T ret = (T)o;
            return ret;
        } catch (ClassCastException e) {
            throw new AssertionError(String.format("Class %s expected assignable to generic type'.", o.getClass()));
        }
    }
}
