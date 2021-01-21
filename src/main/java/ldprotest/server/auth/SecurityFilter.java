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
package ldprotest.server.auth;

import java.util.List;
import java.util.Optional;
import ldprotest.util.types.UrlPathPrefixTree;
import org.eclipse.jetty.http.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import static spark.Spark.before;
import static spark.Spark.halt;

public final class SecurityFilter {

    public final static SecurityFilter INSTANCE = new SecurityFilter();
    private final static Logger LOGGER = LoggerFactory.getLogger(SecurityFilter.class);

    private final UrlPathPrefixTree<FilterData> authTree;

    private SecurityFilter() {
        this.authTree = new UrlPathPrefixTree<>();
    }

    private synchronized void addMethod(String path, SecConfig config) {

        List<String> elements = UrlPathPrefixTree.splitPath(path);

        if(elements.isEmpty()) {
            throw new SecurityConfigException("Empty path not allowed");
        }

        if(!elements.get(0).equals("/")) {
            throw new SecurityConfigException("Path must start with /");
        }

        if(elements.stream().skip(1).anyMatch((elem) -> elem.isEmpty())) {
            LOGGER.info("path {}", path);
            throw new SecurityConfigException("Empty path element not alllowed");
        }

        if(elements.stream().anyMatch((elem) -> elem.contains("*") && !(elem.equals("*") || elem.equals("**")))) {
            throw new SecurityConfigException("Wildcard character '*' not permitted except as standalone '*' or '**'");
        }

        long wildCount = elements.stream().filter((elem) -> elem.equals("*")).count();

        if(wildCount > 1) {
            throw new SecurityConfigException("Only one wildcard element alllowed.");
        }

        String last = elements.get(elements.size() - 1);

        if(wildCount == 1 && !last.contains("*")) {
            throw new SecurityConfigException("Only last path element may be wild.");
        }

        if(authTree.lookup(elements).isPresent()) {
            throw new SecurityConfigException("Security configuration may not be set twice for the same path");
        }

        if(last.equals("*")) {
            elements.remove(elements.size() - 1);
            authTree.add(elements, new FilterData(path, Wildness.WILD, config));
        } else if(last.equals("**")) {
            elements.remove(elements.size() - 1);
            authTree.add(elements, new FilterData(path, Wildness.DOUBLE_WILD, config));
        } else {
            authTree.add(elements, new FilterData(path, Wildness.NOT_WILD, config));
        }
    }

    private Optional<FilterData> lookup(String path) {
        return authTree.longest(path);
    }

    public static void add(String path, SecConfig config) {
        INSTANCE.addMethod(path, config);
    }

    public static void start() {
        before((request, response) -> {
            FilterData data = getFilterDataOrHalt(request.pathInfo());

            HttpVerbTypes method = HttpVerbTypes.fromString(request.requestMethod());
            UserRole role = UserRole.UNAUTHENTICATED;

            if(!data.secConfig.isPermitted(role, method)) {
                LOGGER.warn("Unauthored request from role {} for {} {}", role, method, request.pathInfo());
                throw halt(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
            }
        });
    }

    private static FilterData getFilterDataOrHalt(String path) {
            Optional<FilterData> optData = INSTANCE.lookup(path);

            if(optData.isEmpty()) {
                LOGGER.error("No security data found for path: {}", path);
                throw halt(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
            }

            FilterData data = optData.get();

            if(data.notWild() && path.equals(data.pathSpec)) {
                return data;
            } else if(data.singleWild() && prefix(data.pathSpec).equals(prefix(path))) {
                return data;
            } else if(data.doubleWild()) {
                return data;
            }

            LOGGER.warn("Request for unconfigured subdirectory. Full request path: {}", path);
            throw halt(HttpStatus.UNAUTHORIZED_401, "Unauthorized");
    }

    private static String prefix(String path) {
        return path.substring(path.lastIndexOf("/") + 1);
    }

    private static enum Wildness {
        WILD,
        DOUBLE_WILD,
        NOT_WILD;
    }


    private static class FilterData {

        public final String pathSpec;
        public final Wildness wildness;
        public final SecConfig secConfig;

        public FilterData(String pathSpec, Wildness wildness, SecConfig secConfig) {
            this.pathSpec = pathSpec;
            this.wildness = wildness;
            this.secConfig = secConfig;
        }

        public boolean notWild() {
            return wildness == Wildness.NOT_WILD;
        }

        public boolean singleWild() {
            return wildness == Wildness.WILD;
        }

        public boolean doubleWild() {
            return wildness == Wildness.DOUBLE_WILD;
        }
    }
}
