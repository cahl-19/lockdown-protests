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

import ldprotest.db.DbIndex;
import ldprotest.db.DbSortOrder;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;

public class UserInfo implements JsonSerializable {

    @DbIndex(order = DbSortOrder.ASCENDING, groupId = 0)
    public final String email;

    @DbIndex(order = DbSortOrder.ASCENDING, groupId = 1)
    public final String publicUsername;

    public final UserRole userRole;

    @ReflectiveConstructor
    private UserInfo() {
        publicUsername = "";
        email = "";
        userRole = UserRole.UNAUTHENTICATED;
    }

    public UserInfo(String publicUsername, String email, UserRole userRole) {
        this.publicUsername = publicUsername;
        this.email = email;
        this.userRole = userRole;
    }
}
