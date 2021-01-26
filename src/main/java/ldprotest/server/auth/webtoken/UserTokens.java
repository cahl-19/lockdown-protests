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
package ldprotest.server.auth.webtoken;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import ldprotest.server.auth.UserInfo;
import ldprotest.server.auth.UserRole;
import ldprotest.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserTokens {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserTokens.class);

    private static final int KEY_EXPIRY_SECONDS = 3600 * 24;
    private static final VolatileTokenKeyProvider META_KEY_PROVIDER = new VolatileTokenKeyProvider(
        KEY_EXPIRY_SECONDS
    );

    private UserTokens() {
        /* do not construct */
    }

    public static String sign(UserInfo info) {
        Algorithm algorithm = Algorithm.RSA512(META_KEY_PROVIDER.getKeyProvider());

        return JWT.create()
            .withClaim("username", info.publicUsername)
            .withClaim("email", info.email)
            .withClaim("role", info.userRole.name())
            .sign(algorithm);
    }

    public static Result<String, UserInfo> verify(String token) {
        DecodedJWT jwt;
        try {
            Algorithm algorithm = Algorithm.RSA512(META_KEY_PROVIDER.getKeyProvider());
            JWTVerifier verifier = JWT.require(algorithm)
                .acceptExpiresAt(KEY_EXPIRY_SECONDS)
                .build();
            jwt = verifier.verify(token);
        } catch (JWTVerificationException ex){
            return Result.failure(ex.getMessage());
        }

        try {
            return Result.success(
                new UserInfo(
                    jwt.getClaim("username").asString(),
                    jwt.getClaim("email").asString(),
                    UserRole.valueOf(jwt.getClaim("role").asString())
                )
            );
        }  catch (IllegalArgumentException ex) {
            LOGGER.error("Received Token With Invalid Role: {}", jwt.getClaim("role").asString());
            return Result.failure(ex.getMessage());
        }
    }
}
