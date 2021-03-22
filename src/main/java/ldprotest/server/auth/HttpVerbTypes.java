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
package ldprotest.server.auth;

import ldprotest.util.types.CodedEnum;

public enum HttpVerbTypes implements CodedEnum {
    GET(0),
    POST(1),
    PUT(2),
    DELETE(3),
    OPTIONS(4),
    HEAD(5),
    ANY(6);

    public final int code;

    private HttpVerbTypes(int code) {
        this.code = code;
    }

    @Override
    public int code() {
        return code;
    }

    @Override
    public String description() {
        return name();
    }

    public static HttpVerbTypes fromString(String method) {
        if(method.equals("GET")) {
            return GET;
        } else if((method.equals("POST"))) {
            return POST;
        } else if((method.equals("PUT"))) {
            return PUT;
        } else if((method.equals("DELETE"))) {
            return DELETE;
        } else if((method.equals("OPTIONS"))) {
            return OPTIONS;
        } else if((method.equals("HEAD"))) {
            return HEAD;
        } else {
            throw new IllegalArgumentException("Invalid method string: " + method);
        }
    }
}
