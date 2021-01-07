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

import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;
import spark.ModelAndView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static spark.Spark.get;
import ldprotest.util.TypeTools;

public class ServeTemplate {

    private static final String MAIN_PAGE_TEMPLATE = "infrastructure/page.hbs";
    private static final String TEMPLATE_RESOURCE_PREFIX = "/templates";
    private static final String PAGE_TEMPLATE_BODY_ALIAS = "content";

    private static final Map<String, Object> MAIN_HTML_ATTRIBUTES;

    static {
        MAIN_HTML_ATTRIBUTES = new HashMap<>();

        MAIN_HTML_ATTRIBUTES.put("lang", "en-US");
        MAIN_HTML_ATTRIBUTES.put("charset", "UTF-8");
    }

    public static Page page(String bodyTemplatePath) {
        return new Page(bodyTemplatePath);
    }

    public static final class Page {

        private final String bodyTemplatePath;
        private final Map<String, Object> modelMap;

        public Page(String bodyTemplatePath) {
            this.bodyTemplatePath = bodyTemplatePath;
            this.modelMap = new HashMap<>(MAIN_HTML_ATTRIBUTES);

            this.modelMap.put("scripts", new ArrayList<String>());
            this.modelMap.put("styleSheets", new ArrayList<String>());
        }

        public Page setAttribute(String attribute, String val) {
            modelMap.put(attribute, val);
            return this;
        }

        public Page addScript(String url) {
            List<String> scripts = TypeTools.<List<String>>assertingCast(modelMap.get("scripts"));
            scripts.add(url);
            return this;
        }

        public Page addStyleSheet(String url) {
            List<String> scripts = TypeTools.<List<String>>assertingCast(modelMap.get("styleSheets"));
            scripts.add(url);
            return this;
        }

        public void serve(String url) throws IOException {
            AliasTemplateLoader templateLoader = new AliasTemplateLoader();

            templateLoader.setPrefix(TEMPLATE_RESOURCE_PREFIX);
            templateLoader.setSuffix(null);
            templateLoader.setAlias(PAGE_TEMPLATE_BODY_ALIAS, bodyTemplatePath);

            Handlebars handlebars = new Handlebars(templateLoader);

            handlebars.infiniteLoops(true);
            Template template = handlebars.compile(MAIN_PAGE_TEMPLATE);
            ModelAndView model = new ModelAndView(modelMap, this.bodyTemplatePath);

            get(url, (req, resp) -> template.apply(model.getModel()));
        }
    }
}
