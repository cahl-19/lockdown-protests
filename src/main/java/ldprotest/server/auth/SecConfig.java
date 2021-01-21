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

    private SecConfig(Map<UserRole, EnumSet<HttpVerbTypes>> conf) {
        this.conf = conf;
    }

    public boolean isPermitted(UserRole role, HttpVerbTypes perm) {
        EnumSet<HttpVerbTypes> permissionSet = conf.getOrDefault(role, NO_PERMS);

        return permissionSet.contains(perm) || permissionSet.contains(HttpVerbTypes.ANY);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final Map<UserRole, EnumSet<HttpVerbTypes>> conf;

        private Builder() {
            this.conf = new HashMap<>();
        }

        public Builder add(UserRole role, HttpVerbTypes... perms) {
            EnumSet<HttpVerbTypes> old = conf.put(role, EnumSet.copyOf(Arrays.asList(perms)));

            if(old != null) {
                throw new SecurityConfigException("Role " + role.name() + " already set");
            }

            return this;
        }

        public SecConfig build() {
            return new SecConfig(conf);
        }
    }
}
