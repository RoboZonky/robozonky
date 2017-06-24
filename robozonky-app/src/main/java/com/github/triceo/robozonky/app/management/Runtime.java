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

import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.app.investing.DaemonInvestmentMode;
import com.github.triceo.robozonky.internal.api.Defaults;

class Runtime implements RuntimeMBean {

    private String zonkyUsername = "";
    private OffsetDateTime lastUpdatedDateTime;

    @Override
    public void stopDaemon() {
        DaemonInvestmentMode.BLOCK_UNTIL_RELEASED.release();
    }

    @Override
    public String getZonkyUsername() {
        return this.zonkyUsername;
    }

    void registerInvestmentRun(final ExecutionCompletedEvent event, final SessionInfo sessionInfo) {
        this.lastUpdatedDateTime = event.getCreatedOn();
        this.zonkyUsername = sessionInfo.getUserName();
    }

    @Override
    public void reset() {
        this.zonkyUsername = "";
        this.lastUpdatedDateTime = null;
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
