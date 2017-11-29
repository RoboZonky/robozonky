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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.portfolio.Portfolio;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class InvestmentDaemonTest {

    @Test
    public void standard() {
        final Authenticated a = Mockito.mock(Authenticated.class);
        final Marketplace m = Mockito.mock(Marketplace.class);
        final Supplier<Optional<InvestmentStrategy>> s = Optional::empty;
        InvestingDaemon d = new InvestingDaemon(a, new Investor.Builder(), m, s,
                                                () -> Optional.of(Mockito.mock(Portfolio.class)), Duration.ZERO,
                                                Duration.ofSeconds(1));
        d.run();
        d.run();
        Mockito.verify(m, Mockito.times(2)).run();
        Mockito.verify(m, Mockito.times(1)).registerListener(ArgumentMatchers.any());
        Assertions.assertThat(d.getRefreshInterval()).isEqualByComparingTo(Duration.ofSeconds(1));
    }
}
