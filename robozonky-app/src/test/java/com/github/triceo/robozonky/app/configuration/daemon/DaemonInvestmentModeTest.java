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

package com.github.triceo.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.marketplaces.Marketplace;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.investing.Investor;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class DaemonInvestmentModeTest {

    private static final class TestDaemon implements Daemon {

        private final OffsetDateTime now = OffsetDateTime.now();

        @Override
        public OffsetDateTime getLastRunDateTime() {
            return now;
        }

        @Override
        public void run() {
            // no need to do anything
        }
    }

    @Test
    public void delays1() {
        final Daemon d1 = new TestDaemon();
        final Collection<Daemon> daemons = Arrays.asList(d1);
        Assertions.assertThat(DaemonInvestmentMode.getDelays(daemons, 1))
                .containsEntry(d1, 1000L);
    }

    @Test
    public void delays2() {
        final Daemon d1 = new TestDaemon();
        final Daemon d2 = new TestDaemon();
        final Collection<Daemon> daemons = Arrays.asList(d1, d2);
        Assertions.assertThat(DaemonInvestmentMode.getDelays(daemons, 1))
                .containsEntry(d1, 1000L)
                .containsEntry(d2, 500L);
    }

    @Test
    public void delays3() {
        final Daemon d1 = new TestDaemon();
        final Daemon d2 = new TestDaemon();
        final Daemon d3 = new TestDaemon();
        final Collection<Daemon> daemons = Arrays.asList(d1, d2, d3);
        Assertions.assertThat(DaemonInvestmentMode.getDelays(daemons, 1))
                .containsEntry(d1, 1000L)
                .containsEntry(d2, 667L)
                .containsEntry(d3, 334L);
    }

    @Test
    public void constructor() throws Exception {
        // setup username provider
        final SecretProvider s = SecretProvider.fallback("username");
        final Authenticated a = Mockito.mock(Authenticated.class);
        Mockito.when(a.getSecretProvider()).thenReturn(s);
        // setup dry run determination
        final Investor.Builder b = new Investor.Builder().asDryRun();
        // setup marketplace
        final Marketplace m = Mockito.mock(Marketplace.class);
        try (final DaemonInvestmentMode d = new DaemonInvestmentMode(a, b, true, m, "",
                                                                     Duration.ofMinutes(1), Duration.ofSeconds(1))) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(d.getUsername()).isEqualTo(s.getUsername());
                softly.assertThat(d.isDryRun()).isEqualTo(b.isDryRun());
                softly.assertThat(d.isFaultTolerant()).isEqualTo(true);
            });
        }
        Mockito.verify(m).close();
    }

    @Test
    public void get() throws Exception {
        final SecretProvider s = SecretProvider.fallback("username");
        final Authenticated a = Mockito.mock(Authenticated.class);
        Mockito.when(a.getSecretProvider()).thenReturn(s);
        final Investor.Builder b = new Investor.Builder().asDryRun();
        final Marketplace m = Mockito.mock(Marketplace.class);
        try (final DaemonInvestmentMode d = new DaemonInvestmentMode(a, b, true, m, "",
                                                                     Duration.ofMinutes(1), Duration.ofSeconds(1))) {
            DaemonInvestmentMode.BLOCK_UNTIL_ZERO.get().countDown(); // don't block the thread
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(d.get()).isEqualTo(ReturnCode.OK);
                softly.assertThat(Runtime.getRuntime().removeShutdownHook(d.getShutdownHook())).isTrue();
            });
        }
        Mockito.verify(m).close();
    }
}
