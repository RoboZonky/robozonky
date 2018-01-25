/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.configuration;

import java.time.Duration;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class MarketplaceCommandLineFragmentTest {

    @Test
    public void delayBetweenPrimaryMarketplaceChecks() {
        final MarketplaceCommandLineFragment f = new MarketplaceCommandLineFragment();
        final Duration a = f.getPrimaryMarketplaceCheckDelay();
        Assertions.assertThat(a).isEqualByComparingTo(Duration.ofSeconds(10));
    }

    @Test
    public void delayBetweenSecondaryMarketplaceChecks() {
        final MarketplaceCommandLineFragment f = new MarketplaceCommandLineFragment();
        final Duration a = f.getSecondaryMarketplaceCheckDelay();
        Assertions.assertThat(a).isEqualByComparingTo(Duration.ofSeconds(10));
    }

    @Test
    public void durationForSleeping() {
        final MarketplaceCommandLineFragment f = new MarketplaceCommandLineFragment();
        final Duration a = f.getMaximumSleepDuration();
        Assertions.assertThat(a).isEqualByComparingTo(Duration.ofMinutes(60));
    }
}
