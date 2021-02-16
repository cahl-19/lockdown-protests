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

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public class SecConfig {

    public static final SecConfig ANONYMOUS_GET = SecConfig.builder()
        .add(UserRole.UNAUTHENTICATED, HttpVerbTypes.GET)
        .add(UserRole.MODERATOR, HttpVerbTypes.GET)
        .add(UserRole.PLANNER, HttpVerbTypes.GET)
        .add(UserRole.ADMIN, HttpVerbTypes.GET)
        .build();

    private static final EnumSet<HttpVerbTypes> NO_PERMS = EnumSet.noneOf(HttpVerbTypes.class);

    private final Map<UserRole, EnumSet<HttpVerbTypes>> conf;
    private final boolean authEndpoint;

    private SecConfig(Map<UserRole, EnumSet<HttpVerbTypes>> conf, boolean authEndpoint) {
        this.conf = conf;
        this.authEndpoint = authEndpoint;
    }

    public boolean isPermitted(UserRole role, HttpVerbTypes perm) {
        EnumSet<HttpVerbTypes> permissionSet = conf.getOrDefault(role, NO_PERMS);

        return permissionSet.contains(perm) || permissionSet.contains(HttpVerbTypes.ANY);
    }

    /**
     * Is this endpoint involved in user authentication or session maintenance?
     *
     * Endpoints which are involved in authenticating or modifying the session state of users require special
     * privileges in the user/session security model. In particular, they must be accessible both by clients in the
     * authenticated and unauthenticated states as well as by users with expired tokens.
     *
     * This does not mean that requests to these endpoints should never be blocked under any conditions, however and
     * in some cases (such as in the case of a malformed token) the security infrastructure may block the request.
     *
     * This value **must** not be set true for any endpoint which is not **directly** involved in
     *
     * @return true if this is an auth endpoint
     */
    public boolean isAuthEndpoint() {
        return authEndpoint;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<UserRole, EnumSet<HttpVerbTypes>> conf;
        private boolean authEndpoint;

        private Builder() {
            this.conf = new HashMap<>();
            this.authEndpoint = false;
        }

        public Builder add(UserRole role, HttpVerbTypes... perms) {
            EnumSet<HttpVerbTypes> old = conf.put(role, EnumSet.copyOf(Arrays.asList(perms)));

            if(old != null) {
                throw new SecurityConfigException("Role " + role.name() + " already set");
            }

            return this;
        }

        public Builder setAuthEndpoint() {
            authEndpoint = true;

            return this;
        }

        public SecConfig build() {
            return new SecConfig(conf, authEndpoint);
        }
    }
}
