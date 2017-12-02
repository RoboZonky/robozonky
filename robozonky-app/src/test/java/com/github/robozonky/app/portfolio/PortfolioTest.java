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

package com.github.robozonky.app.portfolio;

import java.time.OffsetDateTime;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatuses;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Settings;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class PortfolioTest extends AbstractZonkyLeveragingTest {

    @Test
    public void cache() {
        final int loanId = 1;
        final Loan loan = new Loan(loanId, 200);
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.when(z.getLoan(ArgumentMatchers.eq(loanId))).thenReturn(loan);
        final Portfolio instance = Portfolio.create(z)
                .orElseThrow(() -> new AssertionError("Should have been present."));
        Assertions.assertThat(instance.getLoan(loanId)).isEmpty();
        // load into cache
        Assertions.assertThat(instance.getLoan(z, loanId)).isSameAs(loan);
        Assertions.assertThat(instance.getLoan(loanId)).contains(loan);
        // make sure item is not reloaded form the API
        Mockito.when(z.getLoan(ArgumentMatchers.anyInt())).thenReturn(null);
        Assertions.assertThat(instance.getLoan(z, loanId)).isSameAs(loan);
        Assertions.assertThat(instance.getLoan(loanId)).contains(loan);
    }

    private static final Investment mock(final boolean isEligible, final boolean isOnSmp) {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getStatus()).thenReturn(InvestmentStatus.ACTIVE);
        Mockito.when(i.isCanBeOffered()).thenReturn(isEligible);
        Mockito.when(i.isOnSmp()).thenReturn(isOnSmp);
        return i;
    }

    private static final Investment mock(final PaymentStatus paymentStatus) {
        final Investment i = mock(true, false);
        Mockito.when(i.getPaymentStatus()).thenReturn(paymentStatus);
        Mockito.when(i.getNextPaymentDate()).thenReturn(OffsetDateTime.now());
        return i;
    }

    private static final Investment mockSold() {
        final Investment i = Mockito.mock(Investment.class);
        Mockito.when(i.getStatus()).thenReturn(InvestmentStatus.SOLD);
        return i;
    }

    @Test
    public void getActiveWithPaymentStatus() {
        final Investment i = mock(PaymentStatus.OK);
        final Investment i2 = mock(PaymentStatus.DUE);
        final Investment i3 = mock(PaymentStatus.WRITTEN_OFF); // ignored because not interested
        final Investment i4 = mockSold(); // ignored because sold
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.when(z.getInvestments()).thenReturn(Stream.of(i, i2, i3, i4));
        final Portfolio instance = Portfolio.create(z)
                .orElseThrow(() -> new AssertionError("Should have been present."));
        final PaymentStatuses p = PaymentStatuses.of(PaymentStatus.OK, PaymentStatus.DUE);
        Assertions.assertThat(instance.getActiveWithPaymentStatus(p)).containsExactly(i, i2);
    }

    @Test
    public void getActiveForSecondaryMarketplace() {
        final Investment i = mock(true, true);
        final Investment i2 = mock(true, false);
        final Investment i3 = mock(false, false);
        final Investment i4 = mock(false, true);
        final Investment i5 = mockSold(); // ignored because sold
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.when(z.getInvestments()).thenReturn(Stream.of(i, i2, i3, i4, i5));
        final Portfolio instance = Portfolio.create(z)
                .orElseThrow(() -> new AssertionError("Should have been present."));
        Assertions.assertThat(instance.getActiveForSecondaryMarketplace()).containsExactly(i2);
    }

    @Test
    public void liveBalance() {
        final Portfolio instance = new Portfolio();
        final int balance = 10_000;
        final Zonky zonky = harmlessZonky(balance);
        Assertions.assertThat(instance.calculateOverview(zonky, false).getCzkAvailable())
                .isEqualTo(balance);
    }

    @Test
    public void dryRunBalance() {
        final Portfolio instance = new Portfolio();
        final int balance = 10_000;
        final Zonky zonky = harmlessZonky(balance - 1);
        System.setProperty(Settings.Key.DEFAULTS_DRY_RUN_BALANCE.getName(), String.valueOf(balance));
        Assertions.assertThat(instance.calculateOverview(zonky, true).getCzkAvailable())
                .isEqualTo(balance);
    }
}
