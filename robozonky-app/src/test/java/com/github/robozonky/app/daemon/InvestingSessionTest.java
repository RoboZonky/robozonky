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

package com.github.robozonky.app.daemon;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.*;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.InvestmentFailureType;
import com.github.robozonky.internal.remote.InvestmentResult;
import com.github.robozonky.internal.remote.Zonky;
import org.jboss.resteasy.specimpl.ResponseBuilderImpl;
import org.junit.jupiter.api.Test;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.ClientErrorException;
import javax.ws.rs.ServiceUnavailableException;
import javax.ws.rs.core.Response;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class InvestingSessionTest extends AbstractZonkyLeveragingTest {

    private static InvestmentStrategy mockStrategy(final int loanToRecommend, final int recommend) {
        return (l, p, r) -> l.stream()
                .filter(i -> i.item().getId() == loanToRecommend)
                .flatMap(i -> i.recommend(Money.from(recommend)).stream());
    }

    @Test
    void constructor() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z);
        final LoanDescriptor ld = mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Collections.singleton(ld);
        final InvestingSession it = new InvestingSession(new LinkedHashSet<>(lds), auth);
        assertSoftly(softly -> {
            softly.assertThat(it.getAvailable()).containsExactly(ld);
            softly.assertThat(it.getResult()).isEmpty();
        });
    }

    @Test
    void makeInvestment() {
        // setup APIs
        final Zonky z = harmlessZonky();
        // run test
        final int amount = 200;
        final LoanDescriptor ld = mockLoanDescriptorWithoutCaptcha();
        final int loanId = ld.item().getId();
        final PowerTenant auth = mockTenant(z);
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, mockLoanDescriptor());
        final Collection<Investment> i = InvestingSession.invest(auth, lds,
                                                                 mockStrategy(loanId, amount));
        // check that one investment was made
        assertThat(i).hasSize(1);
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).hasSize(4);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(ExecutionStartedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(LoanRecommendedEvent.class);
            softly.assertThat(newEvents.get(2)).isInstanceOf(InvestmentMadeEvent.class);
            softly.assertThat(newEvents.get(3)).isInstanceOf(ExecutionCompletedEvent.class);
        });
    }

    @Test
    void failedDueToUnknownError() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z, false);
        final RecommendedLoan r = mockLoanDescriptor().recommend(Money.from(200)).get();
        final Exception thrown = new ServiceUnavailableException();
        doThrow(thrown).when(z).invest(any());
        final InvestingSession t = new InvestingSession(Collections.emptySet(), auth);
        assertThatThrownBy(() -> t.accept(r)).isSameAs(thrown);
    }

    @Test
    void failedDueToTooManyRequests() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z, false);
        final RecommendedLoan r = mockLoanDescriptor().recommend(Money.from(200)).get();
        final Response response = new ResponseBuilderImpl()
                .status(400)
                .entity(InvestmentFailureType.TOO_MANY_REQUESTS.getReason().get())
                .build();
        final ClientErrorException thrown = new BadRequestException(response);
        when(z.invest(any())).thenReturn(InvestmentResult.failure(thrown));
        final InvestingSession t = new InvestingSession(Collections.emptySet(), auth);
        assertThatThrownBy(() -> t.accept(r)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void failedDueToLowBalance() {
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z, false);
        final RecommendedLoan r = mockLoanDescriptor().recommend(Money.from(200)).get();
        final Response response = new ResponseBuilderImpl()
                .status(400)
                .entity(InvestmentFailureType.INSUFFICIENT_BALANCE.getReason().get())
                .build();
        final ClientErrorException thrown = new BadRequestException(response);
        when(z.invest(any())).thenReturn(InvestmentResult.failure(thrown));
        final InvestingSession t = new InvestingSession(Collections.emptySet(), auth);
        assertThat(t.accept(r)).isFalse();
        assertThat(auth.getKnownBalanceUpperBound()).isEqualTo(Money.from(199));
    }

    @Test
    void successful() {
        final int amountToInvest = 200;
        final RecommendedLoan r = mockLoanDescriptor().recommend(Money.from(amountToInvest)).get();
        final Zonky z = harmlessZonky();
        final PowerTenant auth = mockTenant(z);
        final InvestingSession t = new InvestingSession(Collections.emptySet(), auth);
        final boolean result = t.accept(r);
        assertSoftly(softly -> {
            softly.assertThat(result).isTrue();
            softly.assertThat(t.getAvailable()).doesNotContain(r.descriptor());
        });
        final List<Investment> investments = t.getResult();
        assertThat(investments).hasSize(1);
        assertThat(investments.get(0).getAmount()).isEqualTo(Money.from(amountToInvest));
        // validate event sequence
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents)
                .hasSize(1)
                .first()
                .isInstanceOf(InvestmentMadeEvent.class);
    }
}
