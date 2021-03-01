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
package ldprotest.tasks;

import java.util.concurrent.TimeUnit;
import ldprotest.server.auth.UserSessions;

public final class SessionVacuum {
    private static final long PERIOD_HOURS = 1;

    private SessionVacuum() {
        /* do not construct */
    }

    public static void register() {
        PeriodicTaskManager.registerTask(
            PERIOD_HOURS, PERIOD_HOURS, TimeUnit.HOURS, true, (signal) -> UserSessions.deleteExpired()
        );
    }
}
