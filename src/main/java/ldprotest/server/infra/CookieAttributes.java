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

import java.util.Objects;
import spark.Response;

public class CookieAttributes {

    public final String path;
    public final String name;
    public final String value;
    public final int maxAge;
    public final boolean secured;
    public final boolean httpOnly;

    public CookieAttributes(String path, String name, String value, int maxAge, boolean secured, boolean httpOnly) {
        this.path = path;
        this.name = name;
        this.value = value;
        this.maxAge = maxAge;
        this.secured = secured;
        this.httpOnly = httpOnly;
    }

    public void setCookie(Response response) {
        response.cookie(path, name, value, maxAge, secured, httpOnly);
    }

    @Override
    public int hashCode() {
        return Objects.hash(path, name, value, maxAge, secured, httpOnly);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final CookieAttributes other = (CookieAttributes) obj;
        if (this.maxAge != other.maxAge) {
            return false;
        }
        if (this.secured != other.secured) {
            return false;
        }
        if (this.httpOnly != other.httpOnly) {
            return false;
        }
        if (!Objects.equals(this.path, other.path)) {
            return false;
        }
        if (!Objects.equals(this.name, other.name)) {
            return false;
        }
        if (!Objects.equals(this.value, other.value)) {
            return false;
        }
        return true;
    }
}
