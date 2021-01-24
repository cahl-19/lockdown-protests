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

import java.util.Optional;

public class Either<L, R> {

    private final Optional<L> l;
    private final Optional<R> r;

    private Either(Optional<L> l, Optional<R> r) {
        this.l = l;
        this.r = r;
    }

    public L left() {
        return l.get();
    }

    public R right() {
        return r.get();
    }

    public boolean isLeft() {
        return l.isPresent();
    }

    public boolean isRight() {
        return r.isPresent();
    }

    @SuppressWarnings("unchecked")
    public <T> T get(Class<T> clazz) {

        if(l.isPresent()) {
            L left = left();
            if(clazz.isAssignableFrom(left.getClass())) {
                return (T)left;
            }
        } else {
            R right = right();
            if(clazz.isAssignableFrom(right.getClass())) {
                return (T)right;
            }
        }

        throw new IllegalArgumentException("Class " + clazz.getName() + " not present in Either");
    }

    public <T> boolean is(Class<T> clazz) {
        if(l.isPresent()) {
            L left = left();
            if(clazz.isAssignableFrom(left.getClass())) {
                return true;
            }
        } else {
            R right = right();
            if(clazz.isAssignableFrom(right.getClass())) {
                return true;
            }
        }

        return false;
    }

    public static <L, R> Either<L, R> ofLeft(L left) {
        return new Either<>(Optional.of(left), Optional.empty());
    }

    public static <L, R> Either<L, R> ofRight(R right) {
        return new Either<>(Optional.empty(), Optional.of(right));
    }
}
