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

import ldprotest.server.auth.UserInfo;
import ldprotest.server.auth.UserRole;
import ldprotest.server.auth.UserSessionInfo;
import ldprotest.server.auth.webtoken.UserTokens.VerificationFailure;
import ldprotest.util.Result;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class UnitTestUserTokens {

    @Test
    public void testRoundTripToken() {

        UserSessionInfo info = UserSessionInfo.generateSession(
            new UserInfo("test-username-123", "test-email-456", UserRole.MODERATOR)
        );

        String token = UserTokens.sign(info, UserTokenSubject.FOR_COOKIE);
        Result<VerificationFailure, UserSessionInfo> result = UserTokens.verify(token, UserTokenSubject.FOR_COOKIE);

        assertTrue(result.isSuccess());
        assertEquals(info, result.result());
    }

    @Test
    public void testCorruption() {

        UserSessionInfo info = UserSessionInfo.generateSession(
            new UserInfo("test-username-123", "test-email-456", UserRole.MODERATOR)
        );

        String token = UserTokens.sign(info, UserTokenSubject.FOR_COOKIE) + "a";
        Result<VerificationFailure, UserSessionInfo> result = UserTokens.verify(token, UserTokenSubject.FOR_COOKIE);

        assertTrue(result.isFailure());
    }
}
