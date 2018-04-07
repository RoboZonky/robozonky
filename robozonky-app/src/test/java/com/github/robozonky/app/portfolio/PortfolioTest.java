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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.InvestmentBuilder;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatuses;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class PortfolioTest extends AbstractZonkyLeveragingTest {

    private static Investment mockInvestment(final boolean isEligible, final boolean isOnSmp) {
        return buildInvestment(isEligible, isOnSmp).build();
    }

    private static InvestmentBuilder buildInvestment(final boolean isEligible, final boolean isOnSmp) {
        return Investment.custom()
                .setStatus(InvestmentStatus.ACTIVE)
                .setOfferable(isEligible)
                .setOnSmp(isOnSmp)
                .setInWithdrawal(false);
    }

    private static Investment mockInvestment(final PaymentStatus paymentStatus) {
        return buildInvestment(true, false)
                .setPaymentStatus(paymentStatus)
                .setNextPaymentDate(OffsetDateTime.now())
                .build();
    }

    private static Investment mockSold() {
        final Investment i = Investment.custom().build();
        Investment.markAsSold(i);
        return i;
    }

    @Test
    void getActiveWithPaymentStatus() {
        final Investment i = mockInvestment(PaymentStatus.OK);
        final Investment i2 = mockInvestment(PaymentStatus.DUE);
        final Investment i3 = mockInvestment(PaymentStatus.WRITTEN_OFF); // ignored because not interested
        final Investment i4 = mockSold(); // ignored because sold
        final Portfolio instance = new Portfolio(Arrays.asList(i, i2, i3, i4), null);
        final PaymentStatuses p = PaymentStatuses.of(PaymentStatus.OK, PaymentStatus.DUE);
        assertThat(instance.getActiveWithPaymentStatus(p)).containsExactly(i, i2);
    }

    @Test
    void newSale() {
        final Loan l = Loan.custom()
                .setId(1)
                .setAmount(1000)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200);
        final BlockedAmount ba = new BlockedAmount(l.getId(), BigDecimal.valueOf(l.getAmount()),
                                                   TransactionCategory.SMP_SALE_FEE);
        final Zonky z = harmlessZonky(10_000);
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final Authenticated auth = mockAuthentication(z);
        final Portfolio portfolio = new Portfolio(Collections.singletonList(i), mockBalance(z));
        assertThat(portfolio.wasOnceSold(l)).isFalse();
        Investment.putOnSmp(i);
        assertThat(portfolio.wasOnceSold(l)).isTrue();
        portfolio.newBlockedAmount(auth, ba);
        assertSoftly(softly -> {
            softly.assertThat(i.isOnSmp()).isFalse();
            softly.assertThat(i.getStatus()).isEqualTo(InvestmentStatus.SOLD);
        });
        final List<Event> events = this.getNewEvents();
        assertThat(events).first().isInstanceOf(InvestmentSoldEvent.class);
        // doing the same thing again shouldn't do anything
        this.readPreexistingEvents();
        portfolio.newBlockedAmount(auth, ba);
        assertSoftly(softly -> {
            softly.assertThat(i.isOnSmp()).isFalse();
            softly.assertThat(i.getStatus()).isEqualTo(InvestmentStatus.SOLD);
            softly.assertThat(portfolio.wasOnceSold(l)).isTrue();
        });
        final List<Event> newEvents = this.getNewEvents();
        assertThat(newEvents).isEmpty();
    }
}
