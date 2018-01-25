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
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

class SessionTest extends AbstractZonkyLeveragingTest {

    @Test
    public void constructor() {
        final Zonky zonky = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Collections.singleton(ld);
        final Portfolio portfolio = Portfolio.create(zonky);
        final Session it = new Session(portfolio, new LinkedHashSet<>(lds), getInvestor(zonky), zonky);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(it.getAvailable())
                    .isNotSameAs(lds)
                    .containsExactly(ld);
            softly.assertThat(it.getResult()).isEmpty();
        });
    }

    @Test
    public void discardingInvestments() {
        final int loanId = 1, loanId2 = 2;
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor(loanId);
        final LoanDescriptor ld2 = AbstractZonkyLeveragingTest.mockLoanDescriptor(loanId2);
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, ld2, AbstractZonkyLeveragingTest.mockLoanDescriptor());
        // discard the loan
        final SessionState sst = new SessionState(lds);
        sst.discard(ld);
        // setup APIs
        final Zonky zonky = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        Mockito.when(zonky.getLoan(ArgumentMatchers.eq(loanId2))).thenReturn(ld2.item());
        // prepare portfolio that has the other loan as pending
        final Portfolio portfolio = Portfolio.create(zonky);
        portfolio.newBlockedAmount(zonky, new BlockedAmount(loanId2, BigDecimal.valueOf(200),
                                                            TransactionCategory.SMP_BUY));
        // test that the loans are not available
        final Session it = new Session(portfolio, new LinkedHashSet<>(lds), getInvestor(zonky), zonky);
        SoftAssertions.assertSoftly(softly -> {
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

    private Investor getInvestor(final Zonky zonky) {
        return new Investor.Builder().build(zonky);
    }

    @Test
    public void makeInvestment() {
        // setup APIs
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(200);
        // run test
        final int amount = 200;
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptorWithoutCaptcha();
        final int loanId = ld.item().getId();
        Mockito.when(z.getLoan(ArgumentMatchers.eq(loanId))).thenReturn(ld.item());
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractZonkyLeveragingTest.mockLoanDescriptor());
        final Portfolio portfolio = Mockito.spy(Portfolio.create(z));
        final Collection<Investment> i = Session.invest(portfolio, getInvestor(z), z, lds,
                                                        mockStrategy(loanId, amount));
        // check that one investment was made
        Assertions.assertThat(i).hasSize(1);
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(5);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(ExecutionStartedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(LoanRecommendedEvent.class);
            softly.assertThat(newEvents.get(2)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(3)).isInstanceOf(InvestmentMadeEvent.class);
            softly.assertThat(newEvents.get(4)).isInstanceOf(ExecutionCompletedEvent.class);
        });
        Mockito.verify(portfolio).newBlockedAmount(ArgumentMatchers.eq(z),
                                                   ArgumentMatchers.argThat(a -> a.getLoanId() == loanId));
    }

    @Test
    public void underBalance() {
        // setup APIs
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(Defaults.MINIMUM_INVESTMENT_IN_CZK - 1);
        final Portfolio portfolio = Portfolio.create(z);
        // run test
        final Session it = new Session(portfolio, Collections.emptySet(), getInvestor(z), z);
        final Optional<RecommendedLoan> recommendation = AbstractZonkyLeveragingTest.mockLoanDescriptor()
                .recommend(BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK));
        final boolean result = it.invest(recommendation.get());
        // verify result
        Assertions.assertThat(result).isFalse();
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).isEmpty();
    }

    @Test
    public void underAmount() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(0);
        final RecommendedLoan recommendation =
                AbstractZonkyLeveragingTest.mockLoanDescriptor().recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK).get();
        final Portfolio portfolio = Portfolio.create(z);
        final Session t = new Session(portfolio, Collections.singleton(recommendation.descriptor()), getInvestor(z), z);
        final boolean result = t.invest(recommendation);
        // verify result
        Assertions.assertThat(result).isFalse();
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).isEmpty();
    }

    @Test
    public void investmentFailed() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final RecommendedLoan r = AbstractZonkyLeveragingTest.mockLoanDescriptor().recommend(200).get();
        final Exception thrown = new ServiceUnavailableException();
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doThrow(thrown).when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        final Portfolio portfolio = Portfolio.create(z);
        final Session t = new Session(portfolio, Collections.emptySet(), p, z);
        Assertions.assertThatThrownBy(() -> t.invest(r)).isSameAs(thrown);
    }

    @Test
    public void investmentRejected() {
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final RecommendedLoan r = AbstractZonkyLeveragingTest.mockLoanDescriptor().recommend(200).get();
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Portfolio portfolio = Portfolio.create(z);
        final Session t = new Session(portfolio, Collections.emptySet(), p, z);
        final boolean result = t.invest(r);
        Assertions.assertThat(result).isFalse();
        // validate event
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentRejectedEvent.class);
        });
    }

    @Test
    public void investmentDelegated() {
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200).get();
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        final Portfolio portfolio = Portfolio.create(z);
        final Session t = new Session(portfolio, new LinkedHashSet<>(availableLoans), p, z);
        final boolean result = t.invest(r);
        Assertions.assertThat(result).isFalse();
        // validate event
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentDelegatedEvent.class);
        });
    }

    @Test
    public void investmentDelegatedButExpectedConfirmed() {
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200, true).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Portfolio portfolio = Portfolio.create(z);
        final Session t = new Session(portfolio, new LinkedHashSet<>(availableLoans), p, z);
        final boolean result = t.invest(r);
        Assertions.assertThat(result).isFalse();
        // validate event
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentDelegatedEvent.class);
        });
    }

    @Test
    public void investmentIgnoredWhenNoConfirmationProviderAndCaptcha() {
        final LoanDescriptor ld = AbstractZonkyLeveragingTest.mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        // setup APIs
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(10_000);
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.empty()).when(p).getConfirmationProviderId();
        final Portfolio portfolio = Portfolio.create(z);
        final Session t = new Session(portfolio, new LinkedHashSet<>(availableLoans), p, z);
        final boolean result = t.invest(r);
        Assertions.assertThat(result).isFalse();
        // validate event
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentSkippedEvent.class);
        });
    }

    @Test
    public void investmentSuccessful() {
        final int oldBalance = 10_000;
        final int amountToInvest = 200;
        final RecommendedLoan r = AbstractZonkyLeveragingTest.mockLoanDescriptor().recommend(amountToInvest).get();
        final Zonky z = AbstractZonkyLeveragingTest.harmlessZonky(oldBalance);
        Mockito.when(z.getLoan(ArgumentMatchers.eq(r.descriptor().item().getId()))).thenReturn(r.descriptor().item());
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(new ZonkyResponse(amountToInvest))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Portfolio portfolio = Portfolio.create(z);
        final Session t = new Session(portfolio, Collections.emptySet(), p, z);
        final boolean result = t.invest(r);
        Assertions.assertThat(result).isTrue();
        final List<Investment> investments = t.getResult();
        Assertions.assertThat(investments).hasSize(1);
        Assertions.assertThat(investments.get(0).getAmount().intValue()).isEqualTo(amountToInvest);
        // validate event sequence
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentMadeEvent.class);
        });
        // validate event contents
        final InvestmentMadeEvent e = (InvestmentMadeEvent) newEvents.get(1);
        Assertions.assertThat(e.getPortfolioOverview().getCzkAvailable()).isEqualTo(oldBalance - amountToInvest);
    }
}
