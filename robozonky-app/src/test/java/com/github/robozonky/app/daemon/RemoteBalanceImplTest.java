/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.function.Consumer;

import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Settings;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemoteBalanceImplTest extends AbstractZonkyLeveragingTest {

    private static final BigDecimal THOUSAND = BigDecimal.TEN.pow(3);
    private static final String PROPERTY = Settings.Key.DRY_RUN_BALANCE_MINIMUM.getName();

    @Test
    void testDryRun() {
        System.setProperty(PROPERTY, "-1");
        final BigDecimal startingBalance = BigDecimal.valueOf(2_000);
        final Zonky z = harmlessZonky(startingBalance.intValue());
        final Tenant a = mockTenant(z);
        final RefreshableBalance rb = new RefreshableBalance(a);
        rb.run();
        final RemoteBalance b = new RemoteBalanceImpl(rb, true);
        Assertions.assertThat(b.get()).isEqualTo(startingBalance);
        // test some local updates
        b.update(THOUSAND.negate());
        Assertions.assertThat(b.get()).isEqualTo(THOUSAND);
        // make a remote update to ensure local updates are still persisted
        when(z.getWallet()).thenReturn(new Wallet(BigDecimal.ZERO));
        b.update(THOUSAND.negate());
        Assertions.assertThat(b.get()).isEqualTo(BigDecimal.valueOf(-2000));
    }

    @Test
    void testDryRunWithPresetBalance() {
        System.setProperty(PROPERTY, THOUSAND.toString());
        final BigDecimal startingBalance = BigDecimal.valueOf(1_001);
        final Zonky z = harmlessZonky(startingBalance.intValue());
        final Tenant a = mockTenant(z);
        final RefreshableBalance rb = new RefreshableBalance(a);
        rb.run();
        final RemoteBalance b = new RemoteBalanceImpl(rb, true);
        Assertions.assertThat(b.get()).isEqualTo(startingBalance);
        b.update(THOUSAND.negate());
        // minimum is set to be a thousand
        Assertions.assertThat(b.get()).isEqualTo(THOUSAND);
    }

    @Test
    void testDryRunWithZeroMinimumBalance() {
        System.setProperty(PROPERTY, "0");
        final BigDecimal startingBalance = BigDecimal.valueOf(1_000);
        final Zonky z = harmlessZonky(startingBalance.intValue());
        final Tenant a = mockTenant(z);
        final RefreshableBalance rb = new RefreshableBalance(a);
        rb.run();
        final RemoteBalance b = new RemoteBalanceImpl(rb, true);
        Assertions.assertThat(b.get()).isEqualTo(startingBalance);
        b.update(BigDecimal.valueOf(-1_001));
        // minimum is set to be a thousand
        Assertions.assertThat(b.get()).isEqualTo(BigDecimal.ZERO);
    }

    @SuppressWarnings("unchecked")
    @Test
    void closing() throws IOException {
        final Runnable r = mock(Runnable.class);
        final RefreshableBalance rb = new RefreshableBalance(mockTenant());
        RemoteBalance b = new RemoteBalanceImpl(rb, false, mock(Consumer.class), r);
        verify(r, never()).run();
        b.close();
        verify(r).run();
    }

}
