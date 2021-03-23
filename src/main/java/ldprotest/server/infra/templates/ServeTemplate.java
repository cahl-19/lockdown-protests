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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import ldprotest.main.Main;
import ldprotest.main.ServerTime;
import ldprotest.server.infra.HttpCaching;
import ldprotest.server.infra.http.AcceptEncoding;
import ldprotest.util.LruCache;

import static spark.Spark.get;
import ldprotest.util.TypeTools;
import ldprotest.util.types.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static spark.Spark.head;

public class ServeTemplate {

    private static final String MAIN_PAGE_TEMPLATE = "infrastructure/page.hbs";
    private static final String TEMPLATE_RESOURCE_PREFIX = "/templates";
    private static final String PAGE_TEMPLATE_BODY_ALIAS = "content";

    private final static Logger LOGGER = LoggerFactory.getLogger(ServeTemplate.class);

    private static final Map<String, Object> MAIN_HTML_ATTRIBUTES;

    private static final int CACHE_MAX_DATA_SIZE = 1024 * 1024;
    private static final LruCache<Integer, String, String> TEMPLATE_CACHE = new LruCache<>(
        (val, size) -> size + val.length(),
        (val, size) -> size - val.length(),
        (size) -> size > CACHE_MAX_DATA_SIZE,
        0
    );

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
            this.modelMap.put("inlineScript", new ArrayList<String>());
            this.modelMap.put("styleSheets", new ArrayList<String>());
            this.modelMap.put("metaProperty", new ArrayList<Map<String, String>>());
        }

        public Page addMetaProperties(Collection<Pair<String, String>> properties) {
            for(Pair<String, String> p: properties) {
                addMetaProperty(p.first, p.second);
            }
            return this;
        }

        public Page addMetaProperty(String property, String content) {

            Map<String, String> doc = new HashMap<>();
            doc.put("property", property);
            doc.put("content", content);

            List<Map<String, String>> metaProps = TypeTools.<List<Map<String, String>>>assertingCast(
                modelMap.get("metaProperty")
            );
            metaProps.add(doc);

            return this;
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

        public Page addInlineScript(String script) {
            List<String> scripts = TypeTools.<List<String>>assertingCast(modelMap.get("inlineScript"));
            scripts.add(script);
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

            long resourceTimestamp = ServerTime.now().toEpochSecond();

            head(url, (req, resp) -> {

                resp.header("X-Frame-Options", "deny");
                resp.header("Content-Type", "text/html;charset=utf-8");

                if(HttpCaching.needsRefresh(req, resourceTimestamp)) {
                    String body = TEMPLATE_CACHE.computeIfAbsent(url, () -> applyTemplate(template, model));
                    resp.header("Content-Length", Integer.toString(body.length()));
                } else {
                    HttpCaching.setNotModifiedResponse(resp);
                }
                return "";
            });

            get(url, (req, resp) -> {

                resp.header("X-Frame-Options", "deny");
                resp.header("Content-Type", "text/html;charset=utf-8");

                if(HttpCaching.needsRefresh(req, resourceTimestamp)) {

                    if(AcceptEncoding.decode(req.headers("Accept-Encoding")).gzip()) {
                        /* note: the below makes spark automagically gzip the response */
                        resp.header("Content-Encoding", "gzip");
                    }

                    HttpCaching.setCacheHeaders(resp, resourceTimestamp, Main.args().httpCacheMaxAge);
                    return TEMPLATE_CACHE.computeIfAbsent(url, () -> applyTemplate(template, model));
                } else {
                    HttpCaching.setNotModifiedResponse(resp);
                    return "";
                }
            });
        }

        private static String applyTemplate(Template template, ModelAndView model) {
            try {
                return template.apply(model.getModel());
            } catch(IOException ex) {
                LOGGER.error("Error applying template", ex);
                return "";
            }
        }
    }
}
