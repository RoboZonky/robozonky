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
import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.LoanBuilder;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PortfolioTest extends AbstractZonkyLeveragingTest {

    private static final Random RANDOM = new Random();

    private static MyInvestment mockInvestment(final int loanId, final BigDecimal amount) {
        final MyInvestment i = mock(MyInvestment.class);
        when(i.getId()).thenReturn(RANDOM.nextInt());
        when(i.getLoanId()).thenReturn(loanId);
        when(i.getAmount()).thenReturn(amount);
        return i;
    }

    @Test
    void calculateOverview() {
        final BigDecimal investmentSize = BigDecimal.TEN;
        final LoanBuilder l1 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.C);
        final LoanBuilder l2 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.B);
        final LoanBuilder l3 = Loan.custom().setId(RANDOM.nextInt()).setRating(Rating.A);
        final Transaction sold = new Transaction(l2, investmentSize, TransactionCategory.SMP_SELL,
                                                 TransactionOrientation.IN);
        final Zonky z = harmlessZonky(10_000);
        when(z.getTransactions(any())).thenReturn(Stream.of(sold));
        when(z.getBlockedAmounts()).thenReturn(Stream.empty());
        Stream.of(l1, l2, l3).forEach(l -> {
            final Investment i = Investment.fresh(l, investmentSize).build();
            when(z.getLoan(eq(l.getId()))).thenReturn(l.build());
            when(z.getInvestment(eq(l))).thenReturn(Optional.of(i));
        });
        final Tenant t = mockTenant(z);
        final RemoteBalance b = mockBalance(z);
        final Portfolio p = Portfolio.create(t, b);
        // the test starts here
        final PortfolioOverview o = p.calculateOverview();
        assertSoftly(softly -> { // initial portfolio is empty
            softly.assertThat(o.getCzkAvailable()).isEqualTo(10_000);
            softly.assertThat(o.getCzkInvested(Rating.C)).isEqualTo(0);
            softly.assertThat(o.getCzkInvested(Rating.B)).isEqualTo(0);
            softly.assertThat(o.getCzkInvested(Rating.A)).isEqualTo(0);
        });
        p.simulateInvestment(t, l3.getId(), investmentSize);
        p.simulatePurchase(t, l1.getId(), investmentSize);
        p.updateTransactions(t);
        final PortfolioOverview o2 = p.calculateOverview();
        assertSoftly(softly -> {
            // balance minus two investments; sales are not tracked here, it will come in with RemoteBalance
            softly.assertThat(o2.getCzkAvailable()).isEqualTo(9980);
            // check ratings for expenses
            softly.assertThat(o2.getCzkInvested(Rating.C)).isEqualTo(investmentSize.intValue());
            softly.assertThat(o2.getCzkInvested(Rating.A)).isEqualTo(investmentSize.intValue());
            // and the rating for incomes
            softly.assertThat(o2.getCzkInvested(Rating.B)).isEqualTo(investmentSize.negate().intValue());
        });
        List<Event> events = this.getNewEvents();
        assertThat(events).first().isInstanceOf(InvestmentSoldEvent.class);
    }
}
