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

import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

@DisplayName("A portfolio")
class PortfolioTest extends AbstractZonkyLeveragingTest {

    private Zonky zonky;
    private Authenticated authenticated;
    private RemoteBalance balance;

    @BeforeEach
    void mockZonky() {
        zonky = harmlessZonky(10_000);
        authenticated = mockAuthentication(zonky);
        balance = mockBalance(zonky);
    }

    @Test
    @DisplayName("is created.")
    void create() {
        when(zonky.getInvestments((Select) any())).then((i) -> Stream.empty());
        when(zonky.getStatistics()).thenReturn(Statistics.empty());
        final Portfolio p = Portfolio.create(authenticated, balance);
        assertThat(p).isNotNull();
        verify(zonky).getStatistics();
        verify(zonky).getInvestments((Select) any());
    }

    @Nested
    @DisplayName("when created with blocked amounts")
    class HasBlockedAmountsTest extends AbstractZonkyLeveragingTest {

        private final Loan l = Loan.custom()
                .setId(1)
                .setRating(Rating.D)
                .setMyInvestment(mockMyInvestment())
                .build();
        private final Investment i = Investment.fresh(l, 1000)
                .build();
        private final BlockedAmount investment = new BlockedAmount(l.getId(), i.getOriginalPrincipal()),
                smpSale = new BlockedAmount(l.getId(), i.getOriginalPrincipal(), TransactionCategory.SMP_SALE_FEE);
        private Portfolio portfolio;

        @BeforeEach
        void newInstance() {
            portfolio = spy(Portfolio.create(authenticated, balance));
            when(zonky.getBlockedAmounts()).thenAnswer((i) -> Stream.of(investment, smpSale));
            when(zonky.getLoan(eq(l.getId()))).thenReturn(l);
            when(zonky.getInvestment(eq(l.getId()))).thenReturn(Optional.of(i));
            portfolio.updateBlockedAmounts(authenticated); // new blocked amounts must be processed
        }

        @Test
        @DisplayName("properly marks them as seen.")
        void caches() {
            verify(portfolio, times(2)).newBlockedAmount(eq(authenticated), any());
            portfolio.updateBlockedAmounts(authenticated); // no new blocked amount = no new processing
            verify(portfolio, times(2)).newBlockedAmount(eq(authenticated), any());
        }

        @Test
        @DisplayName("triggers event on SMP sale.")
        void triggersSmpSaleEvent() {
            verify(portfolio, times(1)).newBlockedAmount(eq(authenticated), eq(smpSale));
            verify(portfolio).addToBlockedAmounts(eq(l.getRating()), eq(smpSale.getAmount().negate()));
            assertThat(getNewEvents())
                    .hasSize(1)
                    .first().isInstanceOf(InvestmentSoldEvent.class);
        }

        @Test
        @DisplayName("updates balance on new investment.")
        void triggersBalanceUpdateOnNewInvestment() {
            verify(portfolio, times(1)).newBlockedAmount(eq(authenticated), eq(investment));
            verify(portfolio).addToBlockedAmounts(eq(l.getRating()), eq(investment.getAmount()));
        }
    }
}
