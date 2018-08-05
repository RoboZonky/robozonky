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
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.Transaction;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.configuration.daemon.Transactional;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

class PortfolioTest extends AbstractZonkyLeveragingTest {

    @Test
    void calculateOverview() {
        final BigDecimal investmentSize = BigDecimal.TEN;
        final Loan l1 = Loan.custom().setRating(Rating.C).build();
        final Loan l2 = Loan.custom().setRating(Rating.B).build();
        final Loan l3 = Loan.custom().setRating(Rating.A).build();
        final Zonky z = harmlessZonky(10_000);
        Stream.of(l1, l2, l3).forEach(l -> {
            final Investment i = Investment.fresh(l, investmentSize).build();
            when(z.getLoan(eq(l.getId()))).thenReturn(l);
            when(z.getInvestment(eq(l))).thenReturn(Optional.of(i));
        });
        final Tenant t = mockTenant(z);
        final Supplier<TransferMonitor> m = TransferMonitor.createLazy(t);
        final Portfolio p = Portfolio.create(t, m);
        // the test starts here
        final PortfolioOverview o = p.calculateOverview();
        assertSoftly(softly -> { // initial portfolio is empty
            softly.assertThat(o.getCzkAvailable()).isEqualTo(BigDecimal.valueOf(10_000));
            softly.assertThat(o.getCzkInvested(Rating.C)).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(o.getCzkInvested(Rating.B)).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(o.getCzkInvested(Rating.A)).isEqualTo(BigDecimal.ZERO);
        });
        p.simulateInvestment(l3.getId(), l3.getRating(), investmentSize);
        p.simulatePurchase(l1.getId(), l1.getRating(), investmentSize);
        // update transactions remotely and recalculate portfolio
        final Transaction sold = new Transaction(l2, investmentSize, TransactionCategory.SMP_SELL,
                                                 TransactionOrientation.IN);
        when(z.getTransactions((Select) any())).thenAnswer(i -> Stream.of(sold));
        final Transactional transactional = new Transactional(p, t);
        m.get().accept(transactional);
        transactional.run();
        final PortfolioOverview o2 = p.calculateOverview();
        assertSoftly(softly -> {
            // balance minus two investments; sales are not tracked here, it will come in with RemoteBalance
            softly.assertThat(o2.getCzkAvailable()).isEqualTo(BigDecimal.valueOf(9980));
            // check ratings for expenses
            softly.assertThat(o2.getCzkInvested(Rating.C)).isEqualTo(investmentSize);
            softly.assertThat(o2.getCzkInvested(Rating.A)).isEqualTo(investmentSize);
            // and the rating for incomes
            softly.assertThat(o2.getCzkInvested(Rating.B)).isEqualTo(investmentSize.negate());
        });
        final List<Event> events = this.getNewEvents();
        assertThat(events).first().isInstanceOf(InvestmentSoldEvent.class);
    }
}
