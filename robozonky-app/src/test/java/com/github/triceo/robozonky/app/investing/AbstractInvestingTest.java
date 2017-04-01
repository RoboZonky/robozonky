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
import java.time.Duration;
import java.time.OffsetDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.Wallet;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.AbstractEventsAndStateLeveragingTest;
import com.github.triceo.robozonky.app.Events;
import org.junit.Before;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class AbstractInvestingTest extends AbstractEventsAndStateLeveragingTest {

    private static final Random RANDOM = new Random(0);

    protected static Loan mockLoan() {
        return AbstractInvestingTest.mockLoan(AbstractInvestingTest.RANDOM.nextInt());
    }

    protected static Loan mockLoan(final int loanId) {
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getId()).thenReturn(loanId);
        Mockito.when(loan.getRemainingInvestment()).thenReturn(Double.MAX_VALUE);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        return loan;
    }

    protected static LoanDescriptor mockLoanDescriptor() {
        return AbstractInvestingTest.mockLoanDescriptor(Duration.ofSeconds(30));
    }

    protected static LoanDescriptor mockLoanDescriptor(final int loanId) {
        return AbstractInvestingTest.mockLoanDescriptor(loanId, Duration.ofSeconds(30));
    }

    protected static LoanDescriptor mockLoanDescriptor(final TemporalAmount captchaDelay) {
        return AbstractInvestingTest.mockLoanDescriptor(AbstractInvestingTest.RANDOM.nextInt(), captchaDelay);
    }

    protected static LoanDescriptor mockLoanDescriptor(final int loanId, final TemporalAmount captchaDelay) {
        final Loan loan = AbstractInvestingTest.mockLoan(loanId);
        return new LoanDescriptor(loan, captchaDelay);
    }

    protected static ZonkyApi mockApi(final int availableBalance) {
        final ZonkyApi a = Mockito.mock(ZonkyApi.class);
        final BigDecimal balance = BigDecimal.valueOf(availableBalance);
        Mockito.when(a.getWallet()).thenReturn(new Wallet(1, 2, balance, balance));
        Mockito.when(a.getBlockedAmounts(ArgumentMatchers.anyInt(), ArgumentMatchers.anyInt()))
                .thenReturn(Collections.emptyList());
        return a;
    }


    protected List<Event> getNewEvents() {
        return Events.getFired().stream()
                .filter(e -> !previouslyExistingEvents.contains(e))
                .collect(Collectors.toList());
    }

    private Collection<Event> previouslyExistingEvents = new LinkedHashSet<>();

    @Before
    public void readPreexistingEvents() {
        previouslyExistingEvents.addAll(Events.getFired());
    }

}
