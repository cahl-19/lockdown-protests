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

import java.util.Objects;
import java.util.UUID;
import ldprotest.db.DbIndex;
import ldprotest.db.DbSortOrder;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;

public class UserInfo implements JsonSerializable {

    @DbIndex(order = DbSortOrder.ASCENDING, groupId = 0)
    public final String email;

    @DbIndex(order = DbSortOrder.ASCENDING, groupId = 1)
    public final String publicUsername;

    @DbIndex(order = DbSortOrder.ASCENDING, groupId = 2)
    public final UUID globalUniqueId;

    public final UserRole userRole;

    @ReflectiveConstructor
    private UserInfo() {
        publicUsername = "";
        email = "";
        userRole = UserRole.UNAUTHENTICATED;
        globalUniqueId = null;
    }

    private UserInfo(String publicUsername, String email, UserRole userRole, UUID globalUniqueId) {
        this.publicUsername = publicUsername;
        this.email = email;
        this.userRole = userRole;
        this.globalUniqueId = globalUniqueId;
    }

    public static UserInfo generate(String publicUsername, String email, UserRole userRole) {
        return new UserInfo(publicUsername, email, userRole, UUID.randomUUID());
    }

    public static UserInfo fromUserSessionInfo(UserSessionInfo info) {
        return new UserInfo(info.username, info.email, info.role, info.globalUniqueUserId);
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof UserInfo)) {
            return false;
        }

        UserInfo otherInfo = (UserInfo)other;

        return
            publicUsername.equals(otherInfo.publicUsername) &&
            email.equals(otherInfo.email) &&
            userRole.equals(otherInfo.userRole) &&
            globalUniqueId.equals(otherInfo.globalUniqueId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(publicUsername, email, userRole);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("UserInfo(");

        sb.append(" publicUsername=");
        sb.append(publicUsername);

        sb.append(", email=");
        sb.append(email);

        sb.append(" userRole=");
        sb.append(userRole.name());

        sb.append(")");

        return sb.toString();
    }
}
