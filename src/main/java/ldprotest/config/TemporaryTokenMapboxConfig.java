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
package ldprotest.config;

public class TemporaryTokenMapboxConfig {

    public static final int DEFAULT_EXPIRES = 3540;
    public static final int DEFAULT_RENEW = 3180;
    public static final int DEFAULT_CLIENT_REFRESH = 300;

    public static final TemporaryTokenMapboxConfig UNSET_INSTANCE = new TemporaryTokenMapboxConfig(
        "", "", DEFAULT_EXPIRES, DEFAULT_RENEW, DEFAULT_CLIENT_REFRESH
    );

    public final String username;
    public final String accessToken;
    public final int expiresSeconds;
    public final int renewSeconds;
    public final int clientTokenRefreshSeconds;

    public TemporaryTokenMapboxConfig(
        String username, String accessToken, int expiresSeconds, int renewSeconds, int clientTokenRefreshSeconds
    ) {
        this.username = username;
        this.accessToken = accessToken;
        this.expiresSeconds = expiresSeconds;
        this.renewSeconds = renewSeconds;
        this.clientTokenRefreshSeconds = clientTokenRefreshSeconds;
    }

    public  boolean isFullyDefined() {
        return !username.isEmpty() && !accessToken.isEmpty();
    }
}
