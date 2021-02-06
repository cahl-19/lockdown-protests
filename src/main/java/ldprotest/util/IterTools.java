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

import java.util.Iterator;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

public final class IterTools {

    private IterTools() {
        /* Do not construct */
    }

    public static <T, R> Iterable<R> mapIterable(Iterable<T> i, Function<T, R> f) {
        return () -> new Iterator<>() {
            private final Iterator<T> wrapped = i.iterator();
            @Override
            public boolean hasNext() {
                return wrapped.hasNext();
            }

            @Override
            public R next() {
                return f.apply(wrapped.next());
            }
        };
    }

    public static <T> void iterateUntilLast(Iterable<T> iter, Consumer<T> body, Consumer<T> last) {
        Iterator<T> i = iter.iterator();

        if(!i.hasNext()) {
            return;
        }

        while(true) {
            T item = i.next();

            if(i.hasNext()) {
                body.accept(item);
            } else {
                last.accept(item);
                break;
            }
        }
    }

    public static <T, R> R iterateUntilLast(
        R state, Iterable<T> iter, BiFunction<R, T, R> body, BiFunction<R, T, R> last
    ) {
        Iterator<T> i = iter.iterator();

        if(!i.hasNext()) {
            return state;
        }

        while(true) {
            T item = i.next();

            if(i.hasNext()) {
                state = body.apply(state, item);
            } else {
                state = last.apply(state, item);
                break;
            }
        }

        return state;
    }

    public static String mapString(String s, Function<Character, String> func) {
        return s.chars().mapToObj(
            (c) -> func.apply((char)c)
        ).collect(
            StringBuilder::new, (sb, c) -> sb.append(c), (sb1, sb2) -> sb1.append(sb2.toString())
        ).toString();
    }
}
