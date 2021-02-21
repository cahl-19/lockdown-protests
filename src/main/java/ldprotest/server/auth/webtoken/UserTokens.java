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
import com.auth0.jwt.exceptions.InvalidClaimException;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.exceptions.SignatureVerificationException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import com.auth0.jwt.interfaces.DecodedJWT;
import java.util.Date;
import ldprotest.main.Main;
import ldprotest.main.ServerTime;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.util.DateTools;
import ldprotest.util.Result;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UserTokens {

    private final static Logger LOGGER = LoggerFactory.getLogger(UserTokens.class);

    private static final int EXPIRE_LEEWAY_FOR_IGNORE_EXPIRY = 3600 * 24 * 365;
    private static final int NORMAL_EXPIRE_LEEWAY = 15;

    private static final VolatileTokenKeyProvider META_KEY_PROVIDER = new VolatileTokenKeyProvider();

    private UserTokens() {
        /* do not construct */
    }

    public static String sign(UserSessionInfo info, UserTokenSubject subject) {
        Algorithm algorithm = Algorithm.RSA512(META_KEY_PROVIDER.getKeyProvider());
        Date expiry = Date.from(ServerTime.now().plusSeconds(Main.args().tokenExpiresSeconds).toInstant());

        return JWT.create()
            .withClaim("username", info.username)
            .withClaim("email", info.email)
            .withClaim("role", info.role.name())
            .withClaim("sessionId", info.sessionId)
            .withClaim("sessionCreatedAt", DateTools.ZonedDateTimeToDate(info.createdAt))
            .withSubject(subject.name())
            .withExpiresAt(expiry)
            .sign(algorithm);
    }

    public static Result<VerificationFailure, UserSessionInfo> verify(String token, UserTokenSubject subject) {
        return verify(token, subject, NORMAL_EXPIRE_LEEWAY);
    }

    public static Result<VerificationFailure, UserSessionInfo> verifyWithoutExpiration(
        String token, UserTokenSubject subject
    ) {
        return verify(token, subject, EXPIRE_LEEWAY_FOR_IGNORE_EXPIRY);
    }

    private static Result<VerificationFailure, UserSessionInfo> verify(
        String token, UserTokenSubject subject, int expireLeeway
    ) {
        DecodedJWT jwt;
        try {
            try {
                Algorithm algorithm = Algorithm.RSA512(META_KEY_PROVIDER.getKeyProvider());
                JWTVerifier verifier = JWT.require(algorithm)
                    .withSubject(subject.name())
                    .acceptExpiresAt(expireLeeway)
                    .build();
                jwt = verifier.verify(token);
            } catch (InvalidClaimException ex){
                LOGGER.error("Invalid JWT claim: this may be an indication of attempted malicious activity");
                return Result.failure(VerificationFailure.INVALID_CLAIM);
            } catch(SignatureVerificationException ex) {
                LOGGER.warn(
                    "Invalid JWT signature or unknown kid. Message: {}",
                    ex.getMessage()
                );
                return Result.failure(VerificationFailure.SIGNATURE_FAILED);
            } catch(TokenExpiredException ex) {
                return Result.failure(VerificationFailure.EXPIRED);
            } catch (NoSuchKidException ex) {
                return Result.failure(VerificationFailure.NO_KEY);
            } catch(JWTDecodeException ex) {
                return Result.failure(VerificationFailure.MALFORMED_TOKEN);
            }
        } catch(JWTVerificationException ex) {
            LOGGER.error("Unexpected Error in token verification", ex);
            return Result.failure(VerificationFailure.OTHER_ERROR);
        }

        try {
            return Result.success(sessionInfoFromDecodedJWT(jwt));
        }  catch (IllegalArgumentException ex) {
            LOGGER.error("Received Token With Invalid Role: {}", jwt.getClaim("role").asString());
            return Result.failure(VerificationFailure.OTHER_ERROR);
        }
    }

    private static UserSessionInfo sessionInfoFromDecodedJWT(DecodedJWT jwt) {
        return new UserSessionInfo(
            jwt.getClaim("sessionId").asString(),
            jwt.getClaim("username").asString(),
            jwt.getClaim("email").asString(),
            UserRole.valueOf(jwt.getClaim("role").asString()),
            DateTools.DateToZonedDateTime(jwt.getClaim("sessionCreatedAt").asDate())
        );
    }

    public static enum VerificationFailure {
        EXPIRED,
        INVALID_CLAIM,
        SIGNATURE_FAILED,
        OTHER_ERROR,
        MALFORMED_TOKEN,
        NO_KEY;
    }
}
