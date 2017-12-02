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

import java.io.File;
import java.io.IOException;
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
import com.github.robozonky.internal.api.Defaults;
import com.google.common.io.Files;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class DaemonInvestmentModeTest extends AbstractZonkyLeveragingTest {

    private static final String MINIMAL_STRATEGY = "Robot má udržovat konzervativní portfolio.";

    private static File newStrategyFile() throws IOException {
        final File strategy = File.createTempFile("robozonky-strategy", ".cfg");
        Files.write(MINIMAL_STRATEGY, strategy, Defaults.CHARSET);
        return strategy;
    }

    @Test
    public void constructor() throws Exception {
        final Authenticated a = mockAuthentication(Mockito.mock(Zonky.class));
        // setup dry run determination
        final Investor.Builder b = new Investor.Builder().asDryRun();
        // setup marketplace
        final Marketplace m = Mockito.mock(Marketplace.class);
        try (final DaemonInvestmentMode d = new DaemonInvestmentMode(a, new PortfolioUpdater(a), b, true, m, "",
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
        try (final DaemonInvestmentMode d = new DaemonInvestmentMode(a, new PortfolioUpdater(a), b, true, m, "",
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
        try (final DaemonInvestmentMode d = new DaemonInvestmentMode(a, new PortfolioUpdater(a), b, true, m, "",
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
    @Test
    public void loadStrategyAsFile() throws IOException {
        final StrategyProvider r = DaemonInvestmentMode.initStrategy(newStrategyFile().getAbsolutePath());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isPresent();
            softly.assertThat(r.getToPurchase()).isPresent();
        });
    }

    @Test
    public void loadWrongStrategyAsFile() throws IOException {
        final File tmp = File.createTempFile("robozonky-", ".cfg");
        final StrategyProvider r = DaemonInvestmentMode.initStrategy(tmp.getAbsolutePath());
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isEmpty();
            softly.assertThat(r.getToSell()).isEmpty();
            softly.assertThat(r.getToPurchase()).isEmpty();
        });
    }

    @Test
    public void loadStrategyAsUrl() throws IOException {
        final String url = newStrategyFile().toURI().toURL().toString();
        final StrategyProvider r = DaemonInvestmentMode.initStrategy(url);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(r.getToInvest()).isPresent();
            softly.assertThat(r.getToSell()).isPresent();
            softly.assertThat(r.getToPurchase()).isPresent();
        });
    }


}
