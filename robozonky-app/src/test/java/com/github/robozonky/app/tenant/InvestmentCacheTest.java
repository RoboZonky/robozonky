/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.app.tenant;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class InvestmentCacheTest extends AbstractZonkyLeveragingTest {

    @Test
    void emptyGetInvestment() {
        final MyInvestment mi = mock(MyInvestment.class);
        final OffsetDateTime d = OffsetDateTime.now();
        when(mi.getTimeCreated()).thenReturn(d);
        final Loan loan = MockLoanBuilder.fresh();
        final Investment investment = MockInvestmentBuilder.fresh(loan, 200).build();
        final int loanId = loan.getId();
        final Zonky z = harmlessZonky();
        final Tenant t = mockTenant(z);
        final Cache<Investment> c = Cache.forInvestment(t);
        assertThat(c.getFromCache(loanId)).isEmpty(); // nothing returned at first
        when(z.getInvestmentByLoanId(eq(loanId))).thenReturn(Optional.of(investment));
        assertThat(c.get(loanId)).isEqualTo(investment); // return the freshly retrieved investment
    }

    @Test
    void loadLoan() {
        final Instant instant = Instant.now();
        setClock(Clock.fixed(instant, Defaults.ZONE_ID));
        final Loan loan = MockLoanBuilder.fresh();
        final Investment investment = MockInvestmentBuilder.fresh(loan, 200).build();
        final int loanId = loan.getId();
        final Zonky z = harmlessZonky();
        when(z.getInvestmentByLoanId(eq(loanId))).thenReturn(Optional.of(investment));
        final Tenant t = mockTenant(z);
        final Cache<Investment> c = Cache.forInvestment(t);
        assertThat(c.get(loanId)).isEqualTo(investment); // return the freshly retrieved investment
        verify(z).getInvestmentByLoanId(eq(loanId));
        assertThat(c.getFromCache(loanId)).contains(investment);
        verify(z, times(1)).getInvestmentByLoanId(eq(loanId));
        // and now test eviction
        setClock(Clock.fixed(instant.plus(Duration.ofHours(25)), Defaults.ZONE_ID));
        assertThat(c.getFromCache(loanId)).isEmpty();
    }

    @Test
    void fail() {
        final Instant instant = Instant.now();
        setClock(Clock.fixed(instant, Defaults.ZONE_ID));
        final Loan loan = MockLoanBuilder.fresh();
        final int loanId = loan.getId();
        final Zonky z = harmlessZonky();
        when(z.getInvestmentByLoanId(eq(loanId))).thenReturn(Optional.empty());
        final Tenant t = mockTenant(z);
        final Cache<Investment> c = Cache.forInvestment(t);
        assertThatThrownBy(() -> c.get(loanId))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Investment")
                .hasMessageContaining(String.valueOf(loanId));
    }

}
