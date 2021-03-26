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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import ldprotest.config.StyleCustomizationOptions.BannerReadError;
import ldprotest.main.Main;
import ldprotest.server.auth.SecConfig;
import ldprotest.server.auth.SecurityFilter;
import ldprotest.server.endpoints.GeoPin;
import ldprotest.server.endpoints.Login;
import ldprotest.server.endpoints.Logout;
import ldprotest.server.endpoints.Protests;
import ldprotest.server.endpoints.ServerVersion;
import ldprotest.server.endpoints.TokenRefresh;
import ldprotest.server.endpoints.WhoAmI;
import ldprotest.server.endpoints.MapApiToken;
import ldprotest.server.infra.StaticFileServer.GzipMode;
import ldprotest.server.infra.templates.ServeWebpack;
import ldprotest.util.Result;
import spark.Spark;

public class Server {

    private final static Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private static final String WEBPACK_BUNDLES_PREFIX = "webpack-bundles";
    private static final String CSS_PREFIX = "css";
    private static final String ASSETS_PREFIX = "assets";

    public static void start() throws IOException {

        Spark.port(Main.args().serverPort);

        SecurityFilter.start();

        try {
            serveAssets();
            serveStaticPages();
        } catch(IOException e) {
            LOGGER.error("Error loading static pages.");
            throw e;
        }

        serveDynamicEndpoints();
    }

    public static void stop() {
        spark.Spark.stop();
    }

    private static void serveDynamicEndpoints() {
        ServerVersion.register();
        Login.register();
        Logout.register();
        WhoAmI.register();
        TokenRefresh.register();
        GeoPin.register();
        Protests.register();

        serveDynamicTestEndpoints();
    }

    private static void serveAssets() throws IOException {
        StaticFileServer.serve(
            CSS_PREFIX, CSS_PREFIX, ".*\\.css", GzipMode.DYNAMIC_GZIP
        );
        StaticFileServer.serve(
            ASSETS_PREFIX, ASSETS_PREFIX, "(.*\\.png|.*\\.jpg|.*\\.svg)", GzipMode.DYNAMIC_GZIP
        );

        SecurityFilter.add("/" + CSS_PREFIX + "/**", SecConfig.ANONYMOUS_GET);
        SecurityFilter.add("/" + ASSETS_PREFIX + "/**", SecConfig.ANONYMOUS_GET);
    }

    private static void serveStaticPages() throws IOException {
        List<String> scriptBundles = StaticFileServer.serve(
            WEBPACK_BUNDLES_PREFIX, WEBPACK_BUNDLES_PREFIX, "(.*\\.js|.*\\.map)", GzipMode.PRE_GZIP
        );
        Result<BannerReadError, String> bannerHtmlRes = Main.args().styleOptions.bannerHtml();
        ServeWebpack webpacker = new ServeWebpack(scriptBundles);

        if(bannerHtmlRes.isFailure() && !bannerHtmlRes.failureReason().equals(BannerReadError.NOT_DEFINED)) {
            LOGGER.error(
                "Failed to open banner html file {}: {}",
                Main.args().styleOptions.bannerHtmlPath,
                bannerHtmlRes.failureReason().explanation
            );
        }

        webpacker.page("index", Main.args().styleOptions.indexTitle)
            .setAttributeIf("banner", () -> bannerHtmlRes.result(), () -> bannerHtmlRes.isSuccess())
            .addStyleSheetIf(Main.args().styleOptions.cssUrl, () -> !undefinedString(Main.args().styleOptions.cssUrl))
            .addInlineScript(ClientConfig.generateJs())
            .addMetaProperties(Main.args().styleOptions.ogMetaProperties()).serve("/");

        webpacker.page("login").serve("/login");

        SecurityFilter.add("/" + WEBPACK_BUNDLES_PREFIX + "/**", SecConfig.ANONYMOUS_GET);
        SecurityFilter.add("/", SecConfig.ANONYMOUS_GET_AND_HEAD);
        SecurityFilter.add("/login", SecConfig.ANONYMOUS_GET_AND_HEAD);
    }

    private static void serveDynamicTestEndpoints() {
        MapApiToken.register();
    }

    private static boolean undefinedString(String s) {
        return s == null || s.isEmpty();
    }
}
