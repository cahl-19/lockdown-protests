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

import java.security.SecureRandom;
import java.time.ZonedDateTime;
import java.util.Base64;
import java.util.Objects;
import java.util.UUID;
import ldprotest.main.Main;
import ldprotest.main.ServerTime;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.ReflectiveConstructor;

public class UserSessionInfo implements JsonSerializable {

    static private final int SESSION_ID_BYTE_LEN = 32;
    static private final Base64.Encoder B64_ENCODER = Base64.getEncoder();

    public final String sessionId;

    public final String username;
    public final String email;
    public final UserRole role;
    public final UUID globalUniqueUserId;
    public final ZonedDateTime createdAt;

    @ReflectiveConstructor
    private UserSessionInfo() {
        sessionId = null;
        username = null;
        email = null;
        role = null;
        globalUniqueUserId = null;
        createdAt = null;
    }

    public UserSessionInfo(
        String sessionId, String username, String email, UserRole role, UUID globalUniqueUserId, ZonedDateTime createdAt
    ) {
        this.sessionId = sessionId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.globalUniqueUserId = globalUniqueUserId;
        this.createdAt = createdAt;
    }

    public static UserSessionInfo generateSession(UserInfo info) {

        SecureRandom rng = new SecureRandom();
        byte[] sessionId = new byte[SESSION_ID_BYTE_LEN];

        rng.nextBytes(sessionId);

        return new UserSessionInfo(
            B64_ENCODER.encodeToString(sessionId),
            info.publicUsername,
            info.email,
            info.userRole,
            info.globalUniqueId,
            ServerTime.now()
        );
    }

    public boolean expired() {
        return ServerTime.now().isAfter(createdAt.plusSeconds(Main.args().sessionExpiresSeconds));
    }

    public UserInfo toUserInfo() {
        return UserInfo.fromUserSessionInfo(this);
    }

    public UserSessionInfo withNewCreationDate(ZonedDateTime creation) {
        return new UserSessionInfo(sessionId, username, email, role, globalUniqueUserId, creation);
    }

    @Override
    public boolean equals(Object other) {
        if(!(other instanceof UserSessionInfo)) {
            return false;
        }

        UserSessionInfo otherInfo = (UserSessionInfo)other;

        return
            sessionId.equals(otherInfo.sessionId) &&
            username.equals(otherInfo.username) &&
            email.equals(otherInfo.email) &&
            role.equals(otherInfo.role) &&
            createdAt.equals(otherInfo.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            sessionId, username, email, role, createdAt
        );
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("UserSessionInfo(");

        sb.append("username=");
        sb.append(username);

        sb.append(", email=");
        sb.append(email);

        sb.append(", role=");
        sb.append(role.name());

        sb.append(", createdAt=");
        sb.append(createdAt);

        sb.append(", sessionId=");

        if(sessionId == null) {
            sb.append("null");
        } else if(sessionId.isEmpty()) {
            sb.append("<blank>");
        } else {
            sb.append("*****");
        }

        sb.append(")");

        return sb.toString();
    }
}
