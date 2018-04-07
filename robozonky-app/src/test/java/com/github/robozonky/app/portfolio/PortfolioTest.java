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
import java.util.List;
import java.util.Optional;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class PortfolioTest extends AbstractZonkyLeveragingTest {

    @Test
    void newSale() {
        final Loan l = Loan.custom()
                .setId(1)
                .setRating(Rating.D)
                .setAmount(1000)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Investment i = Investment.fresh(l, 200)
                .setId(1)
                .build();
        final BlockedAmount ba = new BlockedAmount(l.getId(), BigDecimal.valueOf(l.getAmount()),
                                                   TransactionCategory.SMP_SALE_FEE);
        final Zonky z = harmlessZonky(10_000);
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        when(z.getInvestment(eq(1))).thenReturn(Optional.of(i));
        final Authenticated auth = mockAuthentication(z);
        final Portfolio portfolio = new Portfolio(Statistics.empty(), new int[0],
                                                  mockBalance(z));
        assertThat(portfolio.wasOnceSold(l)).isFalse();
        portfolio.newBlockedAmount(auth, ba);
        assertThat(portfolio.wasOnceSold(l)).isTrue();
        final List<Event> events = this.getNewEvents();
        assertThat(events).first().isInstanceOf(InvestmentSoldEvent.class);
        // doing the same thing again shouldn't do anything
        this.readPreexistingEvents();
        portfolio.newBlockedAmount(auth, ba);
        assertThat(portfolio.wasOnceSold(l)).isTrue();
        final List<Event> newEvents = this.getNewEvents();
        assertThat(newEvents).isEmpty();
    }
}
