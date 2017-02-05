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

package com.github.triceo.robozonky.app.configuration;

import java.time.Duration;
import java.time.temporal.TemporalAmount;

import com.beust.jcommander.Parameter;

class DaemonModeMarketplaceCommandLineFragment extends MarketplaceCommandLineFragment {

    @Parameter(names = {"-w", "--wait"},
            description = "Number of seconds between consecutive marketplace checks, if applicable.")
    int delayBetweenChecks = 1;

    public TemporalAmount getDelayBetweenChecks() {
        return Duration.ofSeconds(delayBetweenChecks);
    }

}
