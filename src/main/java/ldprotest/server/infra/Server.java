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
import ldprotest.server.infra.templates.ServeWebpack;

public class Server {

    private final static Logger LOGGER = LoggerFactory.getLogger(Server.class);
    private static final String WEBPACK_BUNDLES_PREFIX = "webpack-bundles";
    private static final String CSS_PREFIX = "css";

    public static void start() throws IOException {
        try {

            List<String> scriptBundles = StaticFileServer.serve(
                WEBPACK_BUNDLES_PREFIX, WEBPACK_BUNDLES_PREFIX, "(.*\\.js|.*\\.map)"
            );
            StaticFileServer.serve(
                CSS_PREFIX, CSS_PREFIX, ".*\\.css"
            );

            ServeWebpack webpacker = new ServeWebpack(scriptBundles);

            webpacker.page("index").serve("/");
            
        } catch(IOException e) {
            LOGGER.error("Error loading static pages.");
            throw e;
        }
    }

    public static void stop() {
        spark.Spark.stop();
    }
}
