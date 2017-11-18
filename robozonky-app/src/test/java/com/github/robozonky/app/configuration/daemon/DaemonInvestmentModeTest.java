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

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.common.secrets.SecretProvider;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.Mockito;

public class DaemonInvestmentModeTest {

    private static final class TestDaemon implements Runnable {

        @Override
        public void run() {
            // no need to do anything
        }
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
                                                                     Duration.ofMinutes(1), Duration.ofSeconds(1),
                                                                     Duration.ofSeconds(1))) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(d.getUsername()).isEqualTo(s.getUsername());
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
                                                                     Duration.ofMinutes(1), Duration.ofSeconds(1),
                                                                     Duration.ofSeconds(1))) {
            DaemonInvestmentMode.BLOCK_UNTIL_ZERO.get().countDown(); // don't block the thread
            Assertions.assertThat(d.get()).isEqualTo(ReturnCode.OK);
        }
        Mockito.verify(m).close();
    }
}
