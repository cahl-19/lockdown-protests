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
package ldprotest.business.mapbox;

import java.time.ZonedDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import ldprotest.business.mapbox.MapboxTokenApi.ApiFailure;
import ldprotest.business.mapbox.MapboxTokenApi.TokenData;
import ldprotest.config.RotatingTokenMapboxConfig;
import ldprotest.main.ServerTime;
import ldprotest.tasks.PeriodicTaskManager;
import ldprotest.util.Result;
import ldprotest.util.SimplePattern;
import ldprotest.util.types.MayFail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapboxTokenRotator implements PeriodicTaskManager.PeriodicTask {

    private final static Logger LOGGER = LoggerFactory.getLogger(MapboxTokenRotator.class);

    private static final String TOKEN_NAME_PATTERN_KEY_STRING = "%n";

    private final String username;
    private final String accessToken;
    private final int expires;
    private final int renewal;
    private final List<String> allowedUrls;
    private final String tokenNamePattern;
    private final int serverPollSeconds;

    private final String staticToken;
    private final AtomicReference<String> token;

    public MapboxTokenRotator(RotatingTokenMapboxConfig config, String staticToken) {

        this.username = config.username;
        this.accessToken = config.accessToken;
        this.expires = config.expiresSeconds;
        this.renewal = config.renewSeconds;
        this.allowedUrls = config.allowedUrls;
        this.tokenNamePattern= fixTokenNamePattern(config.tokenNamePattern);
        this.serverPollSeconds = config.serverPollSeconds;

        this.staticToken = staticToken;
        this.token = new AtomicReference<>(staticToken);
    }

    @Override
    public void runTask(PeriodicTaskManager.ShutdownSignal signal) {

        ZonedDateTime now = ServerTime.now();

        Result<ApiFailure, List<TokenData>> listTokenResult = MapboxTokenApi.listPublicTokens(username, accessToken);

        if(listTokenResult.isFailure()) {
            LOGGER.warn("Failed to list API tokens, falling back to static token");
            token.set(staticToken);
            return;
        }

        List<TokenData> ourTokens = listTokenResult.result().stream()
            .filter((d) -> SimplePattern.matchInt(d.note, tokenNamePattern, TOKEN_NAME_PATTERN_KEY_STRING).isSuccess())
            .sorted()
            .collect(Collectors.toList());

        for(TokenData td: ourTokens) {
            if(tokenExpired(td, now)) {
                MapboxTokenApi.deleteToken(username, accessToken, td.id);
            }
        }

        if(timeToRenew(ourTokens, now)) {
            Result<ApiFailure, TokenData> createResult = MapboxTokenApi.createToken(
                username, accessToken, successorTokenName(ourTokens), allowedUrls
            );

            if(createResult.isFailure()) {
                LOGGER.warn("Failed to create new API tokens, falling back to static token");
                token.set(staticToken);
            } else {
                token.set(createResult.result().token);
            }
        } else {
            token.set(ourTokens.get(ourTokens.size() - 1).token);
        }
    }

    public void register() {
        PeriodicTaskManager.registerTask(0, this.serverPollSeconds, TimeUnit.SECONDS, true, this);
    }

    public String get() {
        return token.get();
    }

    private boolean tokenExpired(TokenData td, ZonedDateTime now) {
        return td.created.plusSeconds(expires).isBefore(now);
    }

    private boolean timeToRenew(List<TokenData> sortedTokenList, ZonedDateTime now) {
        if(sortedTokenList.isEmpty()) {
            return true;
        } else {
            return sortedTokenList.get(sortedTokenList.size() - 1).created.plusSeconds(renewal).isBefore(now);
        }
    }

    private String successorTokenName(List<TokenData> sortedTokenList) {

        if(sortedTokenList.isEmpty()) {
            return SimplePattern.replace(tokenNamePattern, TOKEN_NAME_PATTERN_KEY_STRING, 0);
        }

        TokenData td = sortedTokenList.get(sortedTokenList.size() -1);
        MayFail<Integer> result = SimplePattern.matchInt(td.note, tokenNamePattern, TOKEN_NAME_PATTERN_KEY_STRING);

        if(result.isFailure()) {
            return SimplePattern.replace(tokenNamePattern, TOKEN_NAME_PATTERN_KEY_STRING, 0);
        } else {
            return SimplePattern.replace(
                tokenNamePattern, TOKEN_NAME_PATTERN_KEY_STRING, nextRotationNumber(result.result())
            );
        }
    }

    private static int nextRotationNumber(int num) {
        return (num + 1) % 8;
    }

    private static String fixTokenNamePattern(String orig) {
        if(!orig.contains(TOKEN_NAME_PATTERN_KEY_STRING)) {
            return orig + TOKEN_NAME_PATTERN_KEY_STRING;
        } else {
            return orig;
        }
    }
}
