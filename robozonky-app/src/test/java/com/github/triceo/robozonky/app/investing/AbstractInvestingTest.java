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

package com.github.triceo.robozonky.app.investing;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Statistics;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.api.remote.enums.Rating;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.AbstractEventsAndStateLeveragingTest;
import com.github.triceo.robozonky.app.Events;
import com.github.triceo.robozonky.app.portfolio.Portfolio;
import com.github.triceo.robozonky.common.remote.ApiProvider;
import com.github.triceo.robozonky.common.remote.OAuth;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.junit.After;
import org.junit.Before;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AbstractInvestingTest extends AbstractEventsAndStateLeveragingTest {

    private static final Random RANDOM = new Random(0);

    protected static Loan mockLoan(final int loanId) {
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getId()).thenReturn(loanId);
        Mockito.when(loan.getRemainingInvestment()).thenReturn(Double.MAX_VALUE);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        return loan;
    }

    protected static LoanDescriptor mockLoanDescriptor() {
        return AbstractInvestingTest.mockLoanDescriptor(AbstractInvestingTest.RANDOM.nextInt());
    }

    protected static LoanDescriptor mockLoanDescriptor(final int loanId) {
        return AbstractInvestingTest.mockLoanDescriptor(loanId, true);
    }

    protected static LoanDescriptor mockLoanDescriptorWithoutCaptcha() {
        return AbstractInvestingTest.mockLoanDescriptor(AbstractInvestingTest.RANDOM.nextInt(), false);
    }

    protected static LoanDescriptor mockLoanDescriptor(final int loanId, final boolean withCaptcha) {
        final Loan loan = AbstractInvestingTest.mockLoan(loanId);
        if (withCaptcha) {
            Mockito.when(loan.getRating()).thenReturn(Rating.D);
        } else {
            Mockito.when(loan.getRating()).thenReturn(Rating.AAAAA);
        }
        return new LoanDescriptor(loan);
    }

    protected static ApiProvider harmlessApi(final Zonky zonky) {
        final ApiProvider p = Mockito.spy(new ApiProvider());
        Mockito.doReturn(Mockito.mock(OAuth.class)).when(p).oauth();
        Mockito.doReturn(Collections.emptyList()).when(p).marketplace();
        Mockito.doReturn(zonky).when(p).authenticated(ArgumentMatchers.any());
        return p;
    }

    protected static Zonky harmlessZonky(final int availableBalance) {
        final Zonky zonky = Mockito.mock(Zonky.class);
        final BigDecimal balance = BigDecimal.valueOf(availableBalance);
        Mockito.when(zonky.getWallet()).thenReturn(new Wallet(1, 2, balance, balance));
        Mockito.when(zonky.getBlockedAmounts()).thenReturn(Stream.empty());
        Mockito.when(zonky.getStatistics()).thenReturn(new Statistics());
        return zonky;
    }

    protected List<Event> getNewEvents() {
        return Events.getFired().stream()
                .filter(e -> !previouslyExistingEvents.contains(e))
                .collect(Collectors.toList());
    }

    private Collection<Event> previouslyExistingEvents = new LinkedHashSet<>();

    @Before
    @After
    public void clearPortfolioTracker() {
        Portfolio.INSTANCE.reset();
    }

    @Before
    public void readPreexistingEvents() {
        previouslyExistingEvents.addAll(Events.getFired());
    }
}
