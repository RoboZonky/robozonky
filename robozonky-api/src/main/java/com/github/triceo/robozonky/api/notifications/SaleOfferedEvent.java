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

package com.github.triceo.robozonky.api.notifications;

import com.github.triceo.robozonky.api.remote.entities.Investment;

/**
 * Fired immediately after secondary market purchase was submitted to the API.
 */
public final class SaleOfferedEvent extends Event {

    private final Investment investment;
    private final boolean dryRun;

    public SaleOfferedEvent(final Investment investment, final boolean isDryRun) {
        this.investment = investment;
        this.dryRun = isDryRun;
    }

    /**
     * @return The purchase that was made.
     */
    public Investment getInvestment() {
        return this.investment;
    }

    /**
     * @return True if investment was only simulated.
     */
    public boolean isDryRun() {
        return dryRun;
    }
}
