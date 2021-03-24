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
package ldprotest.config;

import java.util.ArrayList;
import java.util.List;
import ldprotest.util.types.Pair;

public class StyleCustomizationOptions {

    public static final StyleCustomizationOptions DEFAULT = new StyleCustomizationOptions(
        "", "", "", "", "", ""
    );

    public final String ogTitle;
    public final String ogType;
    public final String ogDescription;
    public final String ogUrl;
    public final String ogImage;
    public final String indexTitle;

    public StyleCustomizationOptions(
        String ogTitle,
        String ogType,
        String ogDescription,
        String ogUrl,
        String ogImage,
        String indexTitle
    ) {
        this.ogTitle = ogTitle;
        this.ogType = ogType;
        this.ogDescription = ogDescription;
        this.ogUrl = ogUrl;
        this.ogImage = ogImage;
        this.indexTitle = indexTitle;
    }

    public List<Pair<String, String>> ogMetaProperties() {
        List<Pair<String, String>> ret = new ArrayList<>();

        addMetaPropIfSet(ret, "og:title", ogTitle);
        addMetaPropIfSet(ret, "og:type", ogType);
        addMetaPropIfSet(ret, "og:description", ogDescription);
        addMetaPropIfSet(ret, "og:url", ogUrl);
        addMetaPropIfSet(ret, "og:image", ogImage);

        return ret;
    }

    private static void addMetaPropIfSet(List<Pair<String, String>> props, String property, String content) {
        if(content != null && !content.isEmpty()) {
            props.add(new Pair<String, String>(property, content));
        }
    }
}
