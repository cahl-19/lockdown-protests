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
import java.util.function.Supplier;

public class MayFail<R> {
    private final Optional<R> successData;

    private MayFail(Optional<R> successData) {
        this.successData = successData;
    }

    @SuppressWarnings("unchecked")
    public static final <R> MayFail<R> success(R data) {
        return new MayFail(Optional.of(data));
    }

    @SuppressWarnings("unchecked")
    public static final <R> MayFail<R> failure() {
        return new MayFail(Optional.empty());
    }

    public static final <E extends Exception, R> MayFail<R> succeedOrEatException(
        Class<E> exType, Supplier<R> supplier
    ) {
        try {
            return success(supplier.get());
        } catch(Exception ex) {
            if(exType.isInstance(ex)) {
                return failure();
            } else {
                throw ex;
            }
        }
    }

    public boolean isSuccess() {
        return successData.isPresent();
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public R result() {
        return successData.get();
    }

    public Optional<R> toOpt() {
        return successData;
    }
}
