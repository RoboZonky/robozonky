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

package com.github.robozonky.app.daemon.operations;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.ws.rs.ServiceUnavailableException;

import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.api.confirmations.RequestId;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.daemon.BlockedAmountProcessor;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

class InvestingSessionTest extends AbstractZonkyLeveragingTest {

    private static ConfirmationProvider CP = new ConfirmationProvider() {
        @Override
        public boolean requestConfirmation(RequestId auth, int loanId, int amount) {
            return true;
        }

        @Override
        public String getId() {
            return "something";
        }
    };

    @Test
    void constructor() {
        final Zonky z = harmlessZonky(10_000);
        final Tenant auth = mockTenant(z);
        final LoanDescriptor ld = mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Collections.singleton(ld);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final InvestingSession it = new InvestingSession(portfolio, new LinkedHashSet<>(lds), getInvestor(auth), auth);
        assertSoftly(softly -> {
            softly.assertThat(it.getAvailable()).containsExactly(ld);
            softly.assertThat(it.getResult()).isEmpty();
        });
    }

    private static InvestmentStrategy mockStrategy(final int loanToRecommend, final int recommend) {
        return (l, p, r) -> l.stream()
                .filter(i -> i.item().getId() == loanToRecommend)
                .flatMap(i -> i.recommend(BigDecimal.valueOf(recommend)).map(Stream::of).orElse(Stream.empty()));
    }

    private Investor getInvestor(final Tenant auth) {
        return Investor.build(auth);
    }

    @Test
    void makeInvestment() {
        // setup APIs
        final Zonky z = harmlessZonky(200);
        // run test
        final int amount = 200;
        final LoanDescriptor ld = mockLoanDescriptorWithoutCaptcha();
        final int loanId = ld.item().getId();
        final Tenant auth = mockTenant(z);
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, mockLoanDescriptor());
        final Portfolio portfolio = spy(Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth)));
        final Collection<Investment> i = InvestingSession.invest(portfolio, getInvestor(auth), auth, lds,
                                                                 mockStrategy(loanId, amount));
        // check that one investment was made
        assertThat(i).hasSize(1);
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).hasSize(5);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(ExecutionStartedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(LoanRecommendedEvent.class);
            softly.assertThat(newEvents.get(2)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(3)).isInstanceOf(InvestmentMadeEvent.class);
            softly.assertThat(newEvents.get(4)).isInstanceOf(ExecutionCompletedEvent.class);
        });
    }

    @Test
    void underBalance() {
        // setup APIs
        final Zonky z = harmlessZonky(199);
        final Tenant auth = mockTenant(z);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        // run test
        final InvestingSession it = new InvestingSession(portfolio, Collections.emptySet(), getInvestor(auth), auth);
        final Optional<RecommendedLoan> recommendation = mockLoanDescriptor()
                .recommend(BigDecimal.valueOf(200));
        final boolean result = it.invest(recommendation.get());
        // verify result
        assertThat(result).isFalse();
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).isEmpty();
    }

    @Test
    void underAmount() {
        final Zonky z = harmlessZonky(0);
        final Tenant auth = mockTenant(z);
        final RecommendedLoan recommendation =
                mockLoanDescriptor().recommend(200).get();
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final InvestingSession t = new InvestingSession(portfolio, Collections.singleton(recommendation.descriptor()), getInvestor(auth),
                                                        auth);
        final boolean result = t.invest(recommendation);
        // verify result
        assertThat(result).isFalse();
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).isEmpty();
    }

    @Test
    void investmentFailed() {
        final Zonky z = harmlessZonky(10_000);
        final Tenant auth = mockTenant(z);
        final RecommendedLoan r = mockLoanDescriptor().recommend(200).get();
        final Exception thrown = new ServiceUnavailableException();
        final Investor p = mock(Investor.class);
        doThrow(thrown).when(p).invest(eq(r), anyBoolean());
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final InvestingSession t = new InvestingSession(portfolio, Collections.emptySet(), p, auth);
        assertThatThrownBy(() -> t.invest(r)).isSameAs(thrown);
    }

    @Test
    void investmentRejected() {
        final Zonky z = harmlessZonky(10_000);
        final Tenant auth = mockTenant(z);
        final RecommendedLoan r = mockLoanDescriptor().recommend(200).get();
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.of(CP)).when(p).getConfirmationProvider();
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final InvestingSession t = new InvestingSession(portfolio, Collections.emptySet(), p, auth);
        final boolean result = t.invest(r);
        assertSoftly(softly -> {
            softly.assertThat(result).isFalse();
            softly.assertThat(t.getAvailable()).doesNotContain(r.descriptor());
        });
        // validate event
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentRejectedEvent.class);
        });
    }

    @Test
    void investmentDelegated() {
        final LoanDescriptor ld = mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200).get();
        final Zonky z = harmlessZonky(10_000);
        final Tenant auth = mockTenant(z);
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.of(CP)).when(p).getConfirmationProvider();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final InvestingSession t = new InvestingSession(portfolio, new LinkedHashSet<>(availableLoans), p, auth);
        final boolean result = t.invest(r);
        assertSoftly(softly -> {
            softly.assertThat(result).isFalse();
            softly.assertThat(t.getAvailable()).doesNotContain(r.descriptor());
        });
        // validate event
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentDelegatedEvent.class);
        });
    }

    @Test
    void investmentDelegatedButExpectedConfirmed() {
        final LoanDescriptor ld = mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200, true).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        final Zonky z = harmlessZonky(10_000);
        final Tenant auth = mockTenant(z);
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.of(CP)).when(p).getConfirmationProvider();
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final InvestingSession t = new InvestingSession(portfolio, new LinkedHashSet<>(availableLoans), p, auth);
        final boolean result = t.invest(r);
        assertSoftly(softly -> {
            softly.assertThat(result).isFalse();
            softly.assertThat(t.getAvailable()).doesNotContain(r.descriptor());
        });
        // validate event
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentDelegatedEvent.class);
        });
    }

    @Test
    void investmentIgnoredWhenNoConfirmationProviderAndCaptcha() {
        final LoanDescriptor ld = mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        // setup APIs
        final Zonky z = harmlessZonky(10_000);
        final Tenant auth = mockTenant(z);
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.empty()).when(p).getConfirmationProvider();
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final InvestingSession t = new InvestingSession(portfolio, new LinkedHashSet<>(availableLoans), p, auth);
        final boolean result = t.invest(r);
        assertSoftly(softly -> {
            softly.assertThat(result).isFalse();
            softly.assertThat(t.getAvailable()).doesNotContain(r.descriptor());
        });
        // validate event
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentSkippedEvent.class);
        });
    }

    @Test
    void investmentSuccessful() {
        final int oldBalance = 10_000;
        final int amountToInvest = 200;
        final RecommendedLoan r = mockLoanDescriptor().recommend(amountToInvest).get();
        final Zonky z = harmlessZonky(oldBalance);
        final Tenant auth = mockTenant(z);
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(amountToInvest))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.of(CP)).when(p).getConfirmationProvider();
        final Portfolio portfolio = Portfolio.create(auth, BlockedAmountProcessor.createLazy(auth));
        final InvestingSession t = new InvestingSession(portfolio, Collections.emptySet(), p, auth);
        final boolean result = t.invest(r);
        assertSoftly(softly -> {
            softly.assertThat(result).isTrue();
            softly.assertThat(t.getAvailable()).doesNotContain(r.descriptor());
        });
        final List<Investment> investments = t.getResult();
        assertThat(investments).hasSize(1);
        assertThat(investments.get(0).getOriginalPrincipal().intValue()).isEqualTo(amountToInvest);
        // validate event sequence
        final List<Event> newEvents = getEventsRequested();
        assertThat(newEvents).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentMadeEvent.class);
        });
        // validate event contents
        final InvestmentMadeEvent e = (InvestmentMadeEvent) newEvents.get(1);
        assertThat(e.getPortfolioOverview().getCzkAvailable())
                .isEqualTo(BigDecimal.valueOf(oldBalance - amountToInvest));
    }
}
