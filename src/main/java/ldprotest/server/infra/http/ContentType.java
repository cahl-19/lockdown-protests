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
package ldprotest.server.infra.http;

import java.util.Objects;
import java.util.Optional;
import spark.Request;

public class ContentType {
    public final String mime;
    public final Optional<String> charset;
    public final Optional<String> boundary;

    private ContentType(String mime, Optional<String> charset, Optional<String> boundary) {
        this.mime = mime;

        if(charset.isPresent() && boundary.isPresent()) {
            throw new ContentTypeError("Invalid content type");
        }

        this.charset = charset;
        this.boundary = boundary;
    }

    public static ContentType parse(Request request) {
        return parse(request.contentType());
    }

    public static ContentType parse(String value) {

        int len = value.length();
        int startOfField2 = 0;

        for(int i = 0; i < len; i++) {
            if(value.charAt(i) == ' ') {
                throw new ContentTypeError("Invalid content-type: " + value);
            } else if(value.charAt(i) == ';') {
                if(startOfField2 != 0) {
                    throw new ContentTypeError("Too many fields in content-type: " + value);
                } else if((i + 2) >= len) {
                    throw new ContentTypeError("Empty second field in content-type: " + value);
                } else if(value.charAt(i + 1) != ' ') {
                    throw new ContentTypeError("No space following field seperator in content-type: " + value);
                }

                startOfField2 = i + 2;
                i += 1; //advance past the space
            }
        }

        String field1 = value.substring(0, startOfField2 != 0 ? startOfField2 - 2 : len);

        if(startOfField2 != 0) {
            String field2 = value.substring(startOfField2, len);

            int equalsIndex = field2.indexOf('=');

            if(equalsIndex == -1) {
                throw new ContentTypeError("Invalid content-type: " + value);
            }

            String directive = field2.substring(0, equalsIndex);
            String param = field2.substring(equalsIndex + 1);

            if(directive.equals("charset")) {
                return new ContentType(field1, Optional.of(param.toUpperCase()), Optional.empty());
            } else if(directive.equals("boundary")) {
                if(!field1.equals("multipart/form-data")) {
                    throw new ContentTypeError("Boundary without multipart/form-data in content-type: " + value);
                }
                return new ContentType(field1, Optional.empty(), Optional.of(param));
            } else {
                throw new ContentTypeError("Invalid directive in content-type: " + value);
            }
        } else {
            return new ContentType(field1, Optional.empty(), Optional.empty());
        }
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) {
            return false;
        }else if(obj instanceof ContentType) {
            ContentType ct = (ContentType)obj;

            return
                this.mime.equals(ct.mime) &&
                this.charset.equals(ct.charset) &&
                this.boundary.equals(ct.boundary);
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.mime, this.charset, this.boundary);
    }

    public String toString() {
        if(charset.isPresent()) {
            return mime + "; charset=" + charset.get();
        } else if(boundary.isPresent()) {
            return mime + "; boundary=" + boundary.get();
        } else {
            return mime;
        }
    }

    public static class ContentTypeError extends RuntimeException {

        public ContentTypeError(String description) {
            super(description);
        }
    }
}
