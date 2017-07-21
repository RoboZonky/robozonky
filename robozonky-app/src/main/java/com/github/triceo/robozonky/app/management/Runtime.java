/*
 * Copyright 2017 Lukáš Petrovický
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.triceo.robozonky.app.management;

import java.time.OffsetDateTime;
import java.util.concurrent.CountDownLatch;

import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.app.ShutdownEnabler;
import com.github.triceo.robozonky.app.investing.DaemonInvestmentMode;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Runtime implements RuntimeMBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(Runtime.class);

    private String zonkyUsername = "";
    private OffsetDateTime lastUpdatedDateTime;
    private CountDownLatch allowDaemonToTerminate;

    private static void countDown(final CountDownLatch latch) {
        Runtime.LOGGER.debug("Counting down {}.", latch);
        latch.countDown();
    }

    @Override
    public void stopDaemon() {
        Runtime.countDown(DaemonInvestmentMode.BLOCK_UNTIL_ZERO.get()); // signal daemon to end
        /*
         * Graceful shutdown will be assured due to the daemon shutdown hook only being called on normal application
         * shutdown. Therefore we can signal graceful shutdown right away.
         */
        Runtime.countDown(allowDaemonToTerminate);
    }

    @Override
    public String getZonkyUsername() {
        return this.zonkyUsername;
    }

    void registerInvestmentRun(final ExecutionCompletedEvent event, final SessionInfo sessionInfo) {
        this.lastUpdatedDateTime = event.getCreatedOn();
        this.zonkyUsername = sessionInfo.getUserName();
        this.allowDaemonToTerminate = ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.get();
    }

    @Override
    public void reset() {
        this.zonkyUsername = "";
        this.lastUpdatedDateTime = null;
        this.allowDaemonToTerminate = ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.get();
    }

    @Override
    public OffsetDateTime getLatestUpdatedDateTime() {
        return this.lastUpdatedDateTime;
    }

    @Override
    public String getVersion() {
        return Defaults.ROBOZONKY_VERSION;
    }
}
