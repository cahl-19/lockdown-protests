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

package ldprotest.util;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.util.Collections;
import java.util.Iterator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ResourceWalker implements Iterable<String>, AutoCloseable {

    private final FileSystem fileSystem;
    private final String root;
    private final boolean resourceInJar;

    private final static Logger LOGGER = LoggerFactory.getLogger(ResourceWalker.class);

    public ResourceWalker(String path) throws IOException {

        URL resourcePath = classLoader().getResource(path);

        if(resourcePath == null) {
            throw new IOException("Unable to open resource path: " + path);
        }
        URI uri;
        try {
            uri = resourcePath.toURI();
        } catch (URISyntaxException e) {
            throw new IOException(e);
        }

        if(uri.getScheme().equals("jar")) {
            resourceInJar = true;
            fileSystem = FileSystems.newFileSystem(uri, Collections.emptyMap());
        } else {
            resourceInJar = false;
            fileSystem = FileSystems.getDefault();
        }

        if(uri.getPath() == null) {
            root = path.startsWith("/") ? path : "/" + path;
        } else {
            root = uri.getPath().startsWith("/") ? uri.getPath() : "/" + uri.getPath();
        }
    }

    @Override
    public Iterator<String> iterator() {

        Path path = fileSystem.getPath(root);

        try {
            return Files.walk(path)
                .filter((p) -> !Files.isDirectory(p))
                .map((p) -> p.toString())
                .map((p) -> p.substring(root.length() + 1))
                .iterator();

        } catch (IOException e) {
            LOGGER.warn("Error opening resource");
            throw new IllegalStateException("Failed to open resource.");
        }
    }

    @Override
    public void close() throws IOException {
        if(resourceInJar) {
            fileSystem.close();
        }
    }

    private static ClassLoader classLoader() {
        ClassLoader loader = Thread.currentThread().getContextClassLoader();

        if(loader == null) {
            throw new IllegalStateException("Unable to get class loader");
        } else {
            return loader;
        }
    }
}
