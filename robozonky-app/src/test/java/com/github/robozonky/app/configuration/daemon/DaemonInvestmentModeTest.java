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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class DaemonInvestmentModeTest extends AbstractZonkyLeveragingTest {

    @Test
    public void constructor() throws Exception {
        final Authenticated a = mockAuthentication(Mockito.mock(Zonky.class));
        // setup dry run determination
        final Investor.Builder b = new Investor.Builder().asDryRun();
        // setup marketplace
        final Marketplace m = Mockito.mock(Marketplace.class);
        try (final DaemonInvestmentMode d = new DaemonInvestmentMode(a, new PortfolioUpdater(a), b, true, m,
                                                                     Mockito.mock(StrategyProvider.class),
                                                                     Mockito.mock(Runnable.class),
                                                                     Duration.ofMinutes(1), Duration.ofSeconds(1),
                                                                     Duration.ofSeconds(1))) {
            Assertions.assertThat(d.isFaultTolerant()).isTrue();
        }
        Mockito.verify(m).close();
    }

    @Test(timeout = 5000)
    public void get() throws Exception {
        final Authenticated a = mockAuthentication(Mockito.mock(Zonky.class));
        final Investor.Builder b = new Investor.Builder().asDryRun();
        final Marketplace m = Mockito.mock(Marketplace.class);
        final ExecutorService e = Executors.newFixedThreadPool(1);
        final PortfolioUpdater p = Mockito.mock(PortfolioUpdater.class);
        try (final DaemonInvestmentMode d = new DaemonInvestmentMode(a, p, b, true, m,
                                                                     Mockito.mock(StrategyProvider.class),
                                                                     Mockito.mock(Runnable.class),
                                                                     Duration.ofMinutes(1), Duration.ofSeconds(1),
                                                                     Duration.ofSeconds(1))) {
            final Future<ReturnCode> f = e.submit(d::get);
            while (DaemonInvestmentMode.BLOCK_UNTIL_ZERO.get() == null) {
                // do nothing while the parallel task is initializing
            }
            Thread.sleep(1000); // wait for the code to reach the circuit breaker
            DaemonInvestmentMode.BLOCK_UNTIL_ZERO.get().countDown(); // send request to terminate
            Assertions.assertThat(f.get()).isEqualTo(ReturnCode.OK);
        } finally {
            e.shutdownNow();
        }
        Mockito.verify(m).close();
    }

    @Test(timeout = 5000)
    public void getInterrupted() throws Exception {
        final Authenticated a = mockAuthentication(Mockito.mock(Zonky.class));
        final Investor.Builder b = new Investor.Builder().asDryRun();
        final Marketplace m = Mockito.mock(Marketplace.class);
        final ExecutorService e = Executors.newFixedThreadPool(1);
        try (final DaemonInvestmentMode d = new DaemonInvestmentMode(a, new PortfolioUpdater(a), b, true, m,
                                                                     Mockito.mock(StrategyProvider.class),
                                                                     Mockito.mock(Runnable.class),
                                                                     Duration.ofMinutes(1), Duration.ofSeconds(1),
                                                                     Duration.ofSeconds(1))) {
            final Future<ReturnCode> f = e.submit(d::get);
            while (DaemonInvestmentMode.BLOCK_UNTIL_ZERO.get() == null) {
                // do nothing while the parallel task is initializing
            }
            Thread.sleep(1000); // wait for the code to reach the circuit breaker
            f.cancel(true); // make sure it finishes even in an exceptional situation
        } finally {
            e.shutdownNow();
        }
        Mockito.verify(m).close();
    }

}
