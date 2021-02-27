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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class HttpHeader {
    private HttpHeader() {
        /* do not construct */
    }

    public static List<String> directives(String headerContent) {
        List<String> ret = new ArrayList<>();

        for(String directive: headerContent.split(";")) {
            ret.add(directive.strip());
        }

        return ret;
    }

    public static Map<String, String> directiveValues(String headerContent) {
        List<String> directives = directives(headerContent);
        Map<String, String> ret = new HashMap<>();

        for(String directive: directives) {
            String[] dval = directive.split("=");

            if (dval.length == 2) {
                ret.put(dval[0], dval[1]);
            } else if(dval.length == 1) {
                ret.put(dval[0], "");
            } else {
                throw new InvalidDirectiveError("Invalid directive: " + directive);
            }
        }

        return ret;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final List<Directive> directives;

        private Builder() {
            directives = new ArrayList<>();
        }


        public Builder add(String key) {
            return add(key, "");
        }

        public Builder add(String key, String value) {
            directives.add(new Directive(key, value));
            return this;
        }

        public String build() {
            return directives.stream().map((d) -> {
                if(d.value.isEmpty()) {
                    return d.key;
                } else {
                    return d.key + "=" + d.value;
                }
            }).collect(Collectors.joining("; "));
        }
    }

    private static final class Directive {

        public final String key;
        public final String value;

        public Directive(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }

    private static final class InvalidDirectiveError extends RuntimeException {

        public InvalidDirectiveError(String message) {
            super(message);
        }
    }
}
