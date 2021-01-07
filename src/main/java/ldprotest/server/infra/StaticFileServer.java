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
import ldprotest.util.LruCache;
import ldprotest.util.ResourceWalker;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.Pattern;

import static spark.Spark.get;
import ldprotest.util.PathUtils;

public final class StaticFileServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(StaticFileServer.class);
    private static final int CACHE_MAX_DATA_SIZE = 32 * 1024 * 1024;

    private static final Map<String, String> FILE_MAP = new HashMap<>();
    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>();

    private static final LruCache<Integer, String, String> FILE_CACHE = new LruCache<>(
        (val, size) -> size + val.length(),
        (val, size) -> size - val.length(),
        (size) -> size > CACHE_MAX_DATA_SIZE,
        0
    );

    static {
        CONTENT_TYPE_MAP.put("js", "text/javascript;charset=utf-8");
        CONTENT_TYPE_MAP.put("html", "text/html;charset=utf-8");
        CONTENT_TYPE_MAP.put("css", "text/css;charset=utf-8");
    }

    private StaticFileServer() {
        /* do not construct */
    }

    public static void serve(String prefix, String root) throws IOException {
        serve(prefix, root, ".*");
    }

    public static List<String> serve(String prefix, String root, String pattern) throws IOException {
        List<String> servedFiles = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);

        try(ResourceWalker itr = new ResourceWalker(root)) {
            for(String path: itr) {
                if(p.matcher(path).matches()) {
                    String url = urlForFile(prefix, path);
                    String resourcePath = root + "/" + path;

                    FILE_MAP.put(url, resourcePath);
                    servedFiles.add(resourcePath);
                    addRoute(url, resourcePath);
                }
            }
        }

        return servedFiles;
    }

    private static void addRoute(String url, String resourcePath) {
        get(url, (req, resp) -> {
            String content = FILE_CACHE.get(resourcePath);
            String contentType = CONTENT_TYPE_MAP.get(PathUtils.extension(url));

            if(content == null) {
                content = readFile(resourcePath);
                FILE_CACHE.insert(resourcePath, content);
            }

            if(contentType != null) {
                resp.header("Content-Type", contentType);
            }

            return content;
        });
    }

    private static String readFile(String p) {

        try (InputStream stream = classLoader().getResourceAsStream(p)){
            if(stream != null) {
                byte[] bytes = stream.readAllBytes();
                return new String(bytes);
            } else {
                LOGGER.error("Contents of resource {}, are null", p);
                return null;
            }
        }
        catch (IOException e) {
            LOGGER.error("Unable to open resource {].", p);
            return null;
        }
    }

    private static String urlForFile(String prefix, String path) {
        return prefix + "/" + path;
    }

    private static ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }
}
