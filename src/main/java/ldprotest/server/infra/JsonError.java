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
package ldprotest.server.infra;

import java.util.Optional;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;
import ldprotest.util.types.CodedEnum;

public class JsonError implements JsonSerializable {

    private static final JsonError SUCCESS = new JsonError(ServerErrorCode.SUCCESS);
    private static final JsonError INTERNAL_ERRROR = new JsonError(ServerErrorCode.GENERIC_INTERNAL_ERROR);
    private static final JsonError CONTENT_TYPE_ERROR = new JsonError(ServerErrorCode.CONTENT_TYPE_ERROR);


    public final int code;
    public final String description;
    public final Optional<JsonSerializable> details;

    public JsonError(ServerErrorCode errorCode) {
        this(errorCode, Optional.empty());
    }

    public JsonError(ServerErrorCode errorCode, JsonSerializable details) {
        this(errorCode, Optional.of(details));
    }

    private JsonError(ServerErrorCode errorCode, Optional<JsonSerializable> details) {
        this.code = errorCode.code();
        this.description = errorCode.description();
        this.details = details;
    }

    @ReflectiveConstructor
    private JsonError() {
        this.code = 0;
        this.description = "";
        this.details = Optional.empty();
    }

    public static JsonError internalError() {
        return INTERNAL_ERRROR;
    }

     public static JsonError contentTypeError() {
        return CONTENT_TYPE_ERROR;
    }

     public static JsonError success() {
         return SUCCESS;
     }

    public static enum ServerErrorCode implements CodedEnum {
        SUCCESS(0, "success"),
        GENERIC_INTERNAL_ERROR(1, "internal error"),
        CONTENT_TYPE_ERROR(2, "invalid content-type");

        private final int code;
        private final String description;

        private ServerErrorCode(int code, String description) {
            this.code = code;
            this.description = description;
        }

        @Override
        public int code() {
            return code;
        }

        @Override
        public String description() {
            return description;
        }
    }
}
