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
import javax.servlet.ServletOutputStream;

import static spark.Spark.get;
import ldprotest.server.infra.http.AcceptEncoding;
import ldprotest.util.PathUtils;
import ldprotest.main.Main;
import ldprotest.main.ServerTime;
import org.eclipse.jetty.http.HttpStatus;
import spark.Response;
import static spark.Spark.head;

public final class StaticFileServer {

    private final static Logger LOGGER = LoggerFactory.getLogger(StaticFileServer.class);
    private static final int CACHE_MAX_DATA_SIZE = 32 * 1024 * 1024;

    private static final Map<String, String> FILE_MAP = new HashMap<>();
    private static final Map<String, String> CONTENT_TYPE_MAP = new HashMap<>();
    private static final List<String> REQUIRES_XFRAME_OPTIONS = Arrays.asList(
        "htm", "html", "js"
    );

    private static final LruCache<Integer, String, byte[]> FILE_CACHE = new LruCache<>(
        (val, size) -> size + val.length,
        (val, size) -> size - val.length,
        (size) -> size > CACHE_MAX_DATA_SIZE,
        0
    );

    static {
        CONTENT_TYPE_MAP.put("js", "text/javascript;charset=utf-8");
        CONTENT_TYPE_MAP.put("htm", "text/html;charset=utf-8");
        CONTENT_TYPE_MAP.put("html", "text/html;charset=utf-8");
        CONTENT_TYPE_MAP.put("css", "text/css;charset=utf-8");
        CONTENT_TYPE_MAP.put("min.css", "text/css;charset=utf-8");
        CONTENT_TYPE_MAP.put("png", "image/webp");
        CONTENT_TYPE_MAP.put("jpg", "image/webp");
        CONTENT_TYPE_MAP.put("svg", "image/svg+xml");
    }

    private StaticFileServer() {
        /* do not construct */
    }

    public static void serve(String prefix, String root) throws IOException {
        serve(prefix, root, ".*");
    }

    public static List<String> serve(String prefix, String root, String pattern) throws IOException {
        return serve(prefix, root, pattern, GzipMode.NO_GZIP);
    }

    public static List<String> serve(String prefix, String root, String pattern, GzipMode gzipMode) throws IOException {
        List<String> servedFiles = new ArrayList<>();
        Pattern p = Pattern.compile(pattern);

        try(ResourceWalker itr = new ResourceWalker(root)) {
            for(String path: itr) {
                if(p.matcher(path).matches()) {
                    String url = urlForFile(prefix, path);
                    String resourcePath = root + "/" + path;

                    FILE_MAP.put(url, resourcePath);
                    servedFiles.add(resourcePath);
                    addRoute(url, resourcePath, gzipMode);
                }
            }
        }

        return servedFiles;
    }

    private static void addRoute(String url, String resourcePath, GzipMode gzipMode) {
        long timestamp = ServerTime.now().toEpochSecond();

        String extension = PathUtils.extension(url);
        String contentType = CONTENT_TYPE_MAP.get(extension);
        boolean requiresXframeOpt = REQUIRES_XFRAME_OPTIONS.contains(extension);

        head(url, (req, resp) -> {
            resp.header("Content-Type", contentType);
            if(requiresXframeOpt) {
                resp.header("X-Frame-Options", "deny");
            }
            byte[] content = FILE_CACHE.computeIfAbsent(resourcePath, () -> readFile(resourcePath));
            resp.header("Content-Length", content.length);

            return "";
        });

        get(url, (req, resp) -> {

            AcceptEncoding acceptEncoding = AcceptEncoding.decode(req.headers("Accept-Encoding"));
            if(!HttpCaching.needsRefresh(req, timestamp)) {
                HttpCaching.setNotModifiedResponse(resp);
                return "";
            }

            resp.header("Content-Type", contentType);
            if(requiresXframeOpt) {
                resp.header("X-Frame-Options", "deny");
            }

            HttpCaching.setCacheHeaders(resp, timestamp, Main.args().httpCacheMaxAge);

            if(acceptEncoding.gzip() && !gzipMode.equals(GzipMode.NO_GZIP)) {
                resp.header("Content-Encoding", "gzip");
            }

            if(acceptEncoding.gzip() && gzipMode.equals(GzipMode.PRE_GZIP)) {
                String gzipResource = resourcePath + ".gz";
                byte content[] = FILE_CACHE.computeIfAbsent(gzipResource, () -> readFile(gzipResource));
                writeRawBinaryData(resp, content);
                return new byte[0];
            } else {
                byte[] content = FILE_CACHE.computeIfAbsent(resourcePath, () -> readFile(resourcePath));
                resp.header("Content-Length", content.length);
                
                return content;
            }
        });
    }

    private static byte[] readFile(String p) {

        try (InputStream stream = classLoader().getResourceAsStream(p)){
            if(stream != null) {
                return stream.readAllBytes();
            } else {
                LOGGER.error("Contents of resource {}, are null", p);
                return null;
            }
        }
        catch (IOException e) {
            LOGGER.error("Unable to open resource {}.", p);
            return null;
        }
    }

    private static String urlForFile(String prefix, String path) {
        return prefix + "/" + path;
    }

    private static ClassLoader classLoader() {
        return Thread.currentThread().getContextClassLoader();
    }

    private static void writeRawBinaryData(Response resp, byte[] content) {

        resp.status(HttpStatus.OK_200);
        resp.raw().setContentLength(content.length);

        try (ServletOutputStream stream = resp.raw().getOutputStream()) {
            stream.write(content);
            stream.flush();
        }
        catch(IOException e) {
            resp.status(HttpStatus.INTERNAL_SERVER_ERROR_500);
        }
    }

    public enum GzipMode {
        NO_GZIP,
        PRE_GZIP,
        DYNAMIC_GZIP
    }
}
