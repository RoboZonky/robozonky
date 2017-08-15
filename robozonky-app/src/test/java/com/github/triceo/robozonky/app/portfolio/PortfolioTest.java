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

package com.github.triceo.robozonky.app.portfolio;

import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.enums.InvestmentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatus;
import com.github.triceo.robozonky.api.remote.enums.PaymentStatuses;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class PortfolioTest {

    @Before
    @After
    public void reset() {
        Portfolio.INSTANCE.reset();
    }

    @Test
    public void cache() {
        final int loanId = 1;
        final Loan loan = new Loan(loanId, 200);
        Assertions.assertThat(Portfolio.INSTANCE.getLoan(loanId)).isEmpty();
        final Zonky z = Mockito.mock(Zonky.class);
        Mockito.when(z.getLoan(ArgumentMatchers.eq(loanId))).thenReturn(loan);
        // load into cache
        Assertions.assertThat(Portfolio.INSTANCE.getLoan(z, loanId)).isSameAs(loan);
        Assertions.assertThat(Portfolio.INSTANCE.getLoan(loanId)).contains(loan);
        // make sure item is not reloaded form the API
        Mockito.when(z.getLoan(ArgumentMatchers.anyInt())).thenReturn(null);
        Assertions.assertThat(Portfolio.INSTANCE.getLoan(z, loanId)).isSameAs(loan);
        Assertions.assertThat(Portfolio.INSTANCE.getLoan(loanId)).contains(loan);
    }

    @Test
    public void resetting() {
        Portfolio.INSTANCE.reset();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(Portfolio.INSTANCE.getActive()).isEmpty();
            softly.assertThat(Portfolio.INSTANCE.getPending()).isEmpty();
            softly.assertThat(Portfolio.INSTANCE.getLoan(1)).isEmpty();
        });
    }

    @Test
    public void updating() {
        final Consumer<Zonky> fullUpdateNeeded = Mockito.mock(Consumer.class),
                partialUpdateNeeded = Mockito.mock(Consumer.class);
        Portfolio.INSTANCE.registerUpdater(fullUpdateNeeded);
        Portfolio.INSTANCE.registerUpdater(partialUpdateNeeded, Portfolio.UpdateType.PARTIAL);
        final Zonky z = Mockito.mock(Zonky.class);
        Assertions.assertThat(Portfolio.INSTANCE.isUpdating()).isFalse();
        // partial update
        Portfolio.INSTANCE.update(z, Portfolio.UpdateType.PARTIAL);
        Mockito.verify(partialUpdateNeeded).accept(ArgumentMatchers.eq(z));
        Mockito.verify(fullUpdateNeeded, Mockito.never()).accept(ArgumentMatchers.any());
        Mockito.verify(z, Mockito.never()).getInvestments();
        Portfolio.INSTANCE.update(z);
        // full update
        Mockito.verify(partialUpdateNeeded, Mockito.times(2)).accept(ArgumentMatchers.eq(z));
        Mockito.verify(fullUpdateNeeded).accept(ArgumentMatchers.eq(z));
        Mockito.verify(z).getInvestments();
        Assertions.assertThat(Portfolio.INSTANCE.isUpdating()).isFalse();
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
        Portfolio.INSTANCE.update(z);
        final PaymentStatuses p = PaymentStatuses.of(PaymentStatus.OK, PaymentStatus.DUE);
        Assertions.assertThat(Portfolio.INSTANCE.getActiveWithPaymentStatus(p))
                .containsExactly(i, i2);
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
        Portfolio.INSTANCE.update(z);
        Assertions.assertThat(Portfolio.INSTANCE.getActiveForSecondaryMarketplace()).containsExactly(i2);
    }
}
