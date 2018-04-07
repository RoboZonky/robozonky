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

package com.github.robozonky.app.investing;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.ws.rs.ServiceUnavailableException;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.robozonky.api.notifications.LoanRecommendedEvent;
import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class SessionTest extends AbstractZonkyLeveragingTest {

    @Test
    void constructor() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final Authenticated auth = mockAuthentication(z);
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Collections.singleton(ld);
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        final Session it = new Session(portfolio, new LinkedHashSet<>(lds), getInvestor(auth), auth);
        assertSoftly(softly -> {
            softly.assertThat(it.getAvailable())
                    .isNotSameAs(lds)
                    .containsExactly(ld);
            softly.assertThat(it.getResult()).isEmpty();
        });
    }

    @Test
    void discardingInvestments() {
        final int loanId = 1, loanId2 = 2;
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor(loanId);
        final LoanDescriptor ld2 = AbstractZonkyLeveragingTest.mockLoanDescriptor(loanId2);
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, ld2, AbstractZonkyLeveragingTest.mockLoanDescriptor());
        // discard the loan
        final SessionState sst = new SessionState(lds);
        sst.discard(ld);
        // setup APIs
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        when(z.getLoan(eq(loanId2))).thenReturn(ld2.item());
        final Authenticated auth = mockAuthentication(z);
        // prepare portfolio that has the other loan as pending
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        portfolio.newBlockedAmount(auth,
                                   new BlockedAmount(loanId2, BigDecimal.valueOf(200), TransactionCategory.SMP_BUY));
        // test that the loans are not available
        final Session it = new Session(portfolio, new LinkedHashSet<>(lds), getInvestor(auth), auth);
        assertSoftly(softly -> {
            softly.assertThat(it.getAvailable())
                    .hasSize(1)
                    .doesNotContain(ld, ld2);
            softly.assertThat(it.getResult()).isEmpty();
        });
    }

    private RestrictedInvestmentStrategy mockStrategy(final int loanToRecommend, final int recommend) {
        final InvestmentStrategy s = (l, p, r) -> l.stream()
                .filter(i -> i.item().getId() == loanToRecommend)
                .flatMap(i -> i.recommend(BigDecimal.valueOf(recommend)).map(Stream::of).orElse(Stream.empty()));
        return new RestrictedInvestmentStrategy(s, new Restrictions());
    }

    private Investor getInvestor(final Authenticated auth) {
        return new Investor.Builder().build(auth);
    }

    @Test
    void makeInvestment() {
        // setup APIs
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(200);
        // run test
        final int amount = 200;
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptorWithoutCaptcha();
        final int loanId = ld.item().getId();
        when(z.getLoan(eq(loanId))).thenReturn(ld.item());
        final Authenticated auth = mockAuthentication(z);
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractZonkyLeveragingTest.mockLoanDescriptor());
        final Portfolio portfolio = spy(Portfolio.create(auth, mockBalance(z)));
        final Collection<Investment> i = Session.invest(portfolio, getInvestor(auth), auth, lds,
                                                        mockStrategy(loanId, amount));
        // check that one investment was made
        assertThat(i).hasSize(1);
        final List<Event> newEvents = this.getNewEvents();
        assertThat(newEvents).hasSize(5);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(ExecutionStartedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(LoanRecommendedEvent.class);
            softly.assertThat(newEvents.get(2)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(3)).isInstanceOf(InvestmentMadeEvent.class);
            softly.assertThat(newEvents.get(4)).isInstanceOf(ExecutionCompletedEvent.class);
        });
        verify(portfolio).newBlockedAmount(eq(auth), argThat(a -> a.getLoanId() == loanId));
    }

    @Test
    void underBalance() {
        // setup APIs
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(Defaults.MINIMUM_INVESTMENT_IN_CZK - 1);
        final Authenticated auth = mockAuthentication(z);
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        // run test
        final Session it = new Session(portfolio, Collections.emptySet(), getInvestor(auth), auth);
        final Optional<RecommendedLoan> recommendation = AbstractZonkyLeveragingTest.mockLoanDescriptor()
                .recommend(BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK));
        final boolean result = it.invest(recommendation.get());
        // verify result
        assertThat(result).isFalse();
        final List<Event> newEvents = this.getNewEvents();
        assertThat(newEvents).isEmpty();
    }

    @Test
    void underAmount() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(0);
        final Authenticated auth = mockAuthentication(z);
        final RecommendedLoan recommendation =
                AbstractZonkyLeveragingTest.mockLoanDescriptor().recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK).get();
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        final Session t = new Session(portfolio, Collections.singleton(recommendation.descriptor()), getInvestor(auth),
                                      auth);
        final boolean result = t.invest(recommendation);
        // verify result
        assertThat(result).isFalse();
        final List<Event> newEvents = this.getNewEvents();
        assertThat(newEvents).isEmpty();
    }

    @Test
    void investmentFailed() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final Authenticated auth = mockAuthentication(z);
        final RecommendedLoan r = AbstractZonkyLeveragingTest.mockLoanDescriptor().recommend(200).get();
        final Exception thrown = new ServiceUnavailableException();
        final Investor p = mock(Investor.class);
        doThrow(thrown).when(p).invest(eq(r), anyBoolean());
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        final Session t = new Session(portfolio, Collections.emptySet(), p, auth);
        assertThatThrownBy(() -> t.invest(r)).isSameAs(thrown);
    }

    @Test
    void investmentRejected() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final Authenticated auth = mockAuthentication(z);
        final RecommendedLoan r = AbstractZonkyLeveragingTest.mockLoanDescriptor().recommend(200).get();
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        final Session t = new Session(portfolio, Collections.emptySet(), p, auth);
        final boolean result = t.invest(r);
        assertThat(result).isFalse();
        // validate event
        final List<Event> newEvents = this.getNewEvents();
        assertThat(newEvents).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentRejectedEvent.class);
        });
    }

    @Test
    void investmentDelegated() {
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200).get();
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final Authenticated auth = mockAuthentication(z);
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        final Session t = new Session(portfolio, new LinkedHashSet<>(availableLoans), p, auth);
        final boolean result = t.invest(r);
        assertThat(result).isFalse();
        // validate event
        final List<Event> newEvents = this.getNewEvents();
        assertThat(newEvents).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentDelegatedEvent.class);
        });
    }

    @Test
    void investmentDelegatedButExpectedConfirmed() {
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200, true).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final Authenticated auth = mockAuthentication(z);
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        final Session t = new Session(portfolio, new LinkedHashSet<>(availableLoans), p, auth);
        final boolean result = t.invest(r);
        assertThat(result).isFalse();
        // validate event
        final List<Event> newEvents = this.getNewEvents();
        assertThat(newEvents).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentDelegatedEvent.class);
        });
    }

    @Test
    void investmentIgnoredWhenNoConfirmationProviderAndCaptcha() {
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        // setup APIs
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final Authenticated auth = mockAuthentication(z);
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.empty()).when(p).getConfirmationProviderId();
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        final Session t = new Session(portfolio, new LinkedHashSet<>(availableLoans), p, auth);
        final boolean result = t.invest(r);
        assertThat(result).isFalse();
        // validate event
        final List<Event> newEvents = this.getNewEvents();
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
        final RecommendedLoan r = AbstractZonkyLeveragingTest.mockLoanDescriptor().recommend(amountToInvest).get();
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(oldBalance);
        when(z.getLoan(eq(r.descriptor().item().getId()))).thenReturn(r.descriptor().item());
        final Authenticated auth = mockAuthentication(z);
        final Investor p = mock(Investor.class);
        doReturn(new ZonkyResponse(amountToInvest))
                .when(p).invest(eq(r), anyBoolean());
        doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Portfolio portfolio = Portfolio.create(auth, mockBalance(z));
        final Session t = new Session(portfolio, Collections.emptySet(), p, auth);
        final boolean result = t.invest(r);
        assertThat(result).isTrue();
        final List<Investment> investments = t.getResult();
        assertThat(investments).hasSize(1);
        assertThat(investments.get(0).getOriginalPrincipal().intValue()).isEqualTo(amountToInvest);
        // validate event sequence
        final List<Event> newEvents = this.getNewEvents();
        assertThat(newEvents).hasSize(2);
        assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentMadeEvent.class);
        });
        // validate event contents
        final InvestmentMadeEvent e = (InvestmentMadeEvent) newEvents.get(1);
        assertThat(e.getPortfolioOverview().getCzkAvailable()).isEqualTo(oldBalance - amountToInvest);
    }
}
