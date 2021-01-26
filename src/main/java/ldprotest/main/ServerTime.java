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
package ldprotest.main;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class ServerTime {
    private ServerTime() {
        /* do not construct */
    }

    public static ZonedDateTime now() {
        return ZonedDateTime.ofInstant(Instant.now(), ZoneId.of("UTC"));
    }
}
