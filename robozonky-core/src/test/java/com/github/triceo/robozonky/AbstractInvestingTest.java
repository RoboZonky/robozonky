/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Random;

import com.github.triceo.robozonky.api.events.Event;
import com.github.triceo.robozonky.api.events.EventListener;
import com.github.triceo.robozonky.api.events.EventRegistry;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import org.junit.After;
import org.junit.Before;
import org.mockito.Mockito;

public class AbstractInvestingTest {

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
        final Loan loan = AbstractInvestingTest.mockLoan();
        return new LoanDescriptor(loan, Duration.ofSeconds(30));
    }

    private EventListener<Event> listener;

    @Before
    public void registerEventListener() {
        this.listener = Mockito.mock(EventListener.class);
        EventRegistry.INSTANCE.addListener(this.listener);
    }

    protected EventListener<Event> getListener() {
        return listener;
    }

    @After
    public void unregisterEventListener() {
        EventRegistry.INSTANCE.removeListener(this.listener);
        this.listener = null;
    }

}
