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

package com.github.robozonky.app.portfolio;

import java.math.BigDecimal;

import com.github.robozonky.api.remote.entities.Wallet;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Settings;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.when;

class RemoteBalanceImplTest extends AbstractZonkyLeveragingTest {

    private static final BigDecimal THOUSAND = BigDecimal.TEN.pow(3);
    private static final String PROPERTY = Settings.Key.DRY_RUN_BALANCE_MINIMUM.getName();

    @Test
    void testDryRun() {
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
        b.update(THOUSAND.negate());
        Assertions.assertThat(b.get()).isEqualTo(BigDecimal.ZERO);
        // make a remote update to ensure local updates are still persisted
        when(z.getWallet()).thenReturn(new Wallet(startingBalance.subtract(THOUSAND)));
        rb.run(); // register the remote update
        Assertions.assertThat(b.get()).isEqualTo(THOUSAND.negate());
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
}
