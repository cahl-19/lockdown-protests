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
package ldprotest.server.infra.templates;

import java.util.Arrays;
import java.util.List;
import ldprotest.util.PathUtils;

public class ServeWebpack {

    private static final String PAGE_TEMPLATE_ROOT = "pages";

    private final List<String> scriptBundles;

    public ServeWebpack(List<String> scriptBundles) {
        this.scriptBundles = scriptBundles;
    }

    public ServeTemplate.Page page(String name) {
        return page(name, name);
    }

    public ServeTemplate.Page page(String name, String title) {
        ServeTemplate.Page p = ServeTemplate.page(String.format("%s/%s.hbs", PAGE_TEMPLATE_ROOT, name));
        p.setAttribute("title", title);

        for(String bundle : scriptBundles) {
            if(isRequiredVendorScript(name, bundle)) {
                p.addScript(bundle);
            }
        }

        for(String bundle : scriptBundles) {
            if(isMainScript(name, bundle)) {
                p.addScript(bundle);
                break;
            }
        }

        return p;
    }

    private static boolean isMainScript(String name, String script) {
        String filename = PathUtils.basename(script);
        String withoutExt = PathUtils.stripExtension(filename);

        if(!PathUtils.extension(filename).equals("js")) {
            return false;
        }

        return withoutExt.equals(name);
    }

    private static boolean isRequiredVendorScript(String name, String script) {

        String filename = PathUtils.basename(script);
        String withoutExt = PathUtils.stripExtension(filename);
        List<String> components = Arrays.asList(withoutExt.split("~"));

        if(!PathUtils.extension(filename).equals("js")) {
            return false;
        }

        if(components.isEmpty() || !components.get(0).equals("vendors")) {
            return false;
        } else {
            return components.stream().skip(1).anyMatch((c) -> c.equals(name));
        }
    }
}
