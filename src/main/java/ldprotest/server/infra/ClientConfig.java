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

import ldprotest.config.RotatingTokenMapboxConfig;
import ldprotest.config.TemporaryTokenMapboxConfig;
import ldprotest.main.Main;
import ldprotest.serialization.JsonSerializable;
import ldprotest.serialization.JsonSerialization;

public final class ClientConfig {

    private ClientConfig() {
        /* do not construct */
    }

    public  static String generateJs() {

        TemporaryTokenMapboxConfig tempConfig = Main.args().temporaryTokenConfig;
        RotatingTokenMapboxConfig rotConfig = Main.args().rotatingTokenConfig;

        int clientMapboxRenew = 0;
        if(rotConfig.isFullyDefined()) {
            clientMapboxRenew = rotConfig.clientTokenRefreshSeconds;
        } else if(tempConfig.isFullyDefined()) {
            clientMapboxRenew = tempConfig.clientTokenRefreshSeconds;
        }

        StringBuilder sb = new StringBuilder();

        String json = JsonSerialization.GSON.toJson(new JsonConfigObject(
            clientMapboxRenew,
            Main.args().disablePublicLogin
        ));

        sb.append("const CLIENT_CONFIG = ");
        sb.append(json);
        sb.append(";");

        return sb.toString();
    }

    private static final class JsonConfigObject implements JsonSerializable {

        public final int MAP_API_TOKEN_REFRESH_SECONDS;
        public final boolean DISABLE_PUBLIC_LOGIN;

        public JsonConfigObject(int clientMapboxRenew, boolean disablePublicLogin) {
            MAP_API_TOKEN_REFRESH_SECONDS = clientMapboxRenew;
            DISABLE_PUBLIC_LOGIN = disablePublicLogin;
        }
    }
}
