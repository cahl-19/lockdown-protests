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

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import ldprotest.util.IterTools;
import ldprotest.util.types.MayFail;

public final class Sanitize {

    private Sanitize() {
        /* do not construct */
    }

    public static String encodeHtml(String s) {
        return IterTools.mapString(s, (c) -> {
            switch(c.charValue()) {
                case '>':
                    return "&gt;";
                case '<':
                    return "&lt;";
                case '&':
                    return "&amp;";
                case '"':
                    return "&quot;";
                case '\'':
                    return "&#x27;";
                default:
                    return c.toString();
            }
        });
    }

    public static MayFail<String> encodeUrl(String s) {

        try {
            URL url= new URL(s);
            URI uri = new URI(
                url.getProtocol(),
                url.getUserInfo(),
                url.getHost(),
                url.getPort(),
                url.getPath(),
                url.getQuery(),
                url.getRef()
            );
            return MayFail.success(uri.toASCIIString());
        } catch(MalformedURLException | URISyntaxException ex) {
            return MayFail.failure();
        }
    }
}
