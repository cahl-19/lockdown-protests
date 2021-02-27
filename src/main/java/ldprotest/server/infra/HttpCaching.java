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

import org.eclipse.jetty.http.HttpStatus;
import spark.Request;
import spark.Response;

public final class HttpCaching {
    private HttpCaching() {
        /* do not construct */
    }

    public static boolean needsRefresh(Request request, long resourceTimestamp) {
        String etag = request.headers("If-None-Match");

        return etag == null || !etag.equals(Long.toString(resourceTimestamp));
    }

    public static void setNotModifiedResponse(Response response) {
        response.status(HttpStatus.NOT_MODIFIED_304);
    }

    public static void setCacheHeaders(Response response, long resourceTimestamp, long maxAge) {
        response.header(
            "Cache-Control",
            HttpHeader.builder()
                .add("max-age", Long.toString(maxAge))
                .add("must-revalidate")
                .build()
        );

        response.header("Etag",
            Long.toString(resourceTimestamp)
        );
    }
}
