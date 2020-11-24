/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Zonky;

class InvestingSessionTest extends AbstractZonkyLeveragingTest {

    private static InvestmentStrategy mockStrategy(final int loanToRecommend, final int recommend) {
        return (l, p, r) -> l.item()
            .getId() == loanToRecommend ? Optional.of(Money.from(recommend)) : Optional.empty();
    }

    @Test
    void constructor() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z);
        final LoanDescriptor ld = mockLoanDescriptor();
        final InvestingSession it = new InvestingSession(Stream.of(ld), auth);
        assertSoftly(softly -> {
            softly.assertThat(it.getAvailable())
                .containsExactly(ld);
            softly.assertThat(it.getResult())
                .isEmpty();
        });
    }

    @Test
    void makeInvestment() {
        // setup APIs
        final Zonky z = harmlessZonky();
        // run test
        final int amount = 200;
        final LoanDescriptor ld = mockLoanDescriptor();
        final int loanId = ld.item()
            .getId();
        final PowerTenant auth = mockTenant(z);
        final Stream<LoanDescriptor> lds = Stream.of(ld, mockLoanDescriptor());
        final Stream<Loan> i = InvestingSession.invest(auth, lds, mockStrategy(loanId, amount));
        // check that one investment was made
        assertThat(i).hasSize(1);
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).hasSize(3);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0))
                .isInstanceOf(ExecutionStartedEvent.class);
            softly.assertThat(newEvents.get(1))
                .isInstanceOf(InvestmentMadeEvent.class);
            softly.assertThat(newEvents.get(2))
                .isInstanceOf(ExecutionCompletedEvent.class);
        });
        verify(auth).setKnownBalanceUpperBound(eq(Money.from(Integer.MAX_VALUE - 200)));
    }

    @Test
    void failedDueToUnknownError() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z, false);
        final RecommendedLoan r = new RecommendedLoan(mockLoanDescriptor(), Money.from(200));
        final Exception thrown = new ServiceUnavailableException();
        doThrow(thrown).when(z)
            .invest(notNull(), anyInt());
        final InvestingSession t = new InvestingSession(Stream.empty(), auth);
        assertThatThrownBy(() -> t.accept(r)).isInstanceOf(IllegalStateException.class);
        verify(auth, never()).setKnownBalanceUpperBound(any());
    }

    @Test
    void failedDueToTooManyRequests() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z, false);
        final RecommendedLoan r = new RecommendedLoan(mockLoanDescriptor(), Money.from(200));
        final Response response = Response.status(400)
            .entity("TOO_MANY_REQUESTS")
            .build();
        final ClientErrorException thrown = new BadRequestException(response);
        doThrow(thrown).when(z)
            .invest(any(), anyInt());
        final InvestingSession t = new InvestingSession(Stream.empty(), auth);
        assertThatThrownBy(() -> t.accept(r)).isInstanceOf(IllegalStateException.class);
        verify(auth, never()).setKnownBalanceUpperBound(any());
    }

    @Test
    void failedDueToLowBalance() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z, false);
        final RecommendedLoan r = new RecommendedLoan(mockLoanDescriptor(), Money.from(200));
        final Response response = Response.status(400)
            .entity("insufficientBalance")
            .build();
        final ClientErrorException thrown = new BadRequestException(response);
        doThrow(thrown).when(z)
            .invest(any(), anyInt());
        final InvestingSession t = new InvestingSession(Stream.empty(), auth);
        assertThat(t.accept(r)).isFalse();
        assertThat(auth.getKnownBalanceUpperBound()).isEqualTo(Money.from(199));
    }

    @Test
    void successful() {
        final int amountToInvest = 200;
        final RecommendedLoan r = new RecommendedLoan(mockLoanDescriptor(), Money.from(amountToInvest));
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z);
        final InvestingSession t = new InvestingSession(Stream.empty(), auth);
        final boolean result = t.accept(r);
        assertSoftly(softly -> {
            softly.assertThat(result)
                .isTrue();
            softly.assertThat(t.getAvailable())
                .doesNotContain(r.descriptor());
        });
        final Stream<Loan> investments = t.getResult();
        assertThat(investments).hasSize(1);
        // validate event sequence
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents)
            .hasSize(1)
            .first()
            .isInstanceOf(InvestmentMadeEvent.class);
        verify(auth).setKnownBalanceUpperBound(eq(Money.from(Integer.MAX_VALUE - 200)));
    }

}
