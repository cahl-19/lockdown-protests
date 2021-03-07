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

import java.util.Optional;

public final class Result<E,R> {
    private final Optional<E> failCause;
    private final Optional<R> successData;

    private Result(Optional<E> failCause, Optional<R> successData) {
        this.failCause = failCause;
        this.successData = successData;
    }

    @SuppressWarnings("unchecked")
    public static final <E, R> Result<E, R> success(R data) {
        return new Result(Optional.empty(), Optional.of(data));
    }

    @SuppressWarnings("unchecked")
    public static final <E, R> Result<E, R> failure(E reason) {
        return new Result(Optional.of(reason), Optional.empty());
    }

    public boolean isSuccess() {
        return successData.isPresent();
    }

    public boolean isFailure() {
        return !isSuccess();
    }

    public E failureReason() {
        return failCause.get();
    }

    public R result() {
        return successData.get();
    }


    @Override
    public String toString() {
        if(isSuccess()) {
            return "Result(Success=true, Result=" + successData.get() + ")";
        } else {
            return "Result(Success=false, Failure=" + successData.get() + ")";
        }
    }
}
