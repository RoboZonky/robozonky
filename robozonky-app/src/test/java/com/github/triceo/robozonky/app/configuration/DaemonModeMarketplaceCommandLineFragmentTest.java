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

import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAmount;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class DaemonModeMarketplaceCommandLineFragmentTest {

    @Test
    public void durationBetweenChecks() {
        final DaemonModeMarketplaceCommandLineFragment f = new DaemonModeMarketplaceCommandLineFragment();
        final TemporalAmount a = f.getDelayBetweenChecks();
        Assertions.assertThat(a.get(ChronoUnit.SECONDS)).isEqualTo(f.delayBetweenChecks);
    }

    @Test
    public void durationForSleeping() {
        final DaemonModeMarketplaceCommandLineFragment f = new DaemonModeMarketplaceCommandLineFragment();
        final TemporalAmount a = f.getMaximumSleepDuration();
        Assertions.assertThat(a.get(ChronoUnit.SECONDS)).isEqualTo(f.maximumSleepDuration * 60);
    }
}
