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
package ldprotest.tasks.maintenance;

import com.mongodb.MongoException;
import java.util.concurrent.TimeUnit;
import ldprotest.business.PrivateProtestData;
import ldprotest.tasks.PeriodicTaskManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ProtestVacuum {

    private final static Logger LOGGER = LoggerFactory.getLogger(ProtestVacuum.class);

    private static final long RUN_PERIOD_HOURS = 1;
    private static final long EXPIRED_PROTEST_KEEP_TIME_MS = 12 * 3600 * 1000;

    private ProtestVacuum() {
        /* do not construct */
    }

    public static void register() {

        PeriodicTaskManager.registerTask(
            RUN_PERIOD_HOURS, RUN_PERIOD_HOURS, TimeUnit.HOURS, true, (signal) -> {

                try {
                    long count = PrivateProtestData.deleteOlderThan(EXPIRED_PROTEST_KEEP_TIME_MS);
                    if(count != 0) {
                        LOGGER.info("Protest Vacuum deleted {} protests", count);
                    }
                } catch(MongoException ex) {
                    LOGGER.warn("Error thrown during attempted protest deletion", ex);
                }
            }
        );
    }
}
