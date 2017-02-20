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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.ws.rs.ServiceUnavailableException;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

/**
 * This test mocks all the possible outcomes of a single investment, so that the higher-level investing testing can
 * assume no problems.
 */
public class WireInvestorTest extends AbstractInvestingTest {

    @Test
    public void underBalance() {
        final BigDecimal currentBalance = BigDecimal.valueOf(0);
        final InvestmentTracker t = new InvestmentTracker(Collections.emptyList(), currentBalance);
        final Optional<Recommendation> recommendation =
                AbstractInvestingTest.mockLoanDescriptor().recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK);
        final Optional<Investment> result = Investor.actuallyInvest(recommendation.get(), null, t);
        // verify result
        Assertions.assertThat(result).isEmpty();
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).isEmpty();
    }

    @Test
    public void underAmount() {
        final BigDecimal currentBalance = BigDecimal.valueOf(100);
        final Recommendation recommendation =
                AbstractInvestingTest.mockLoanDescriptor().recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK).get();
        final InvestmentTracker t =
                new InvestmentTracker(Collections.singletonList(recommendation.getLoanDescriptor()), currentBalance);
        final Optional<Investment> result = Investor.actuallyInvest(recommendation, null, t);
        // verify result
        Assertions.assertThat(result).isEmpty();
        Assertions.assertThat(t.isSeenBefore(recommendation.getLoanDescriptor().getLoan().getId())).isFalse();
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).isEmpty();
    }

    @Test
    public void investmentFailed() {
        final InvestmentTracker t = new InvestmentTracker(Collections.emptyList(), BigDecimal.valueOf(10000));
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        final ZonkyProxy api = Mockito.mock(ZonkyProxy.class);
        final Exception thrown = new ServiceUnavailableException();
        Mockito.doThrow(thrown).when(api).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Assertions.assertThatThrownBy(() -> Investor.actuallyInvest(r, api, t)).isSameAs(thrown);
    }

    @Test
    public void investmentRejected() {
        final InvestmentTracker t = new InvestmentTracker(Collections.emptyList(), BigDecimal.valueOf(10000));
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        final ZonkyProxy api = Mockito.mock(ZonkyProxy.class);
        Mockito.when(api.invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean()))
                .thenReturn(new ZonkyResponse(ZonkyResponseType.REJECTED));
        Mockito.when(api.getConfirmationProviderId()).thenReturn(Optional.of("something"));
        Assertions.assertThat(t.isSeenBefore(r.getLoanDescriptor().getLoan().getId())).isFalse();
        final Optional<Investment> result = Investor.actuallyInvest(r, api, t);
        Assertions.assertThat(result).isEmpty();
        Assertions.assertThat(t.isSeenBefore(r.getLoanDescriptor().getLoan().getId())).isTrue();
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
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        final InvestmentTracker t = new InvestmentTracker(availableLoans, BigDecimal.valueOf(10000));
        final Recommendation r = ld.recommend(200).get();
        final int loanId = ld.getLoan().getId();
        final ZonkyProxy api = Mockito.mock(ZonkyProxy.class);
        Mockito.when(api.invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean()))
                .thenReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED));
        Mockito.when(api.getConfirmationProviderId()).thenReturn(Optional.of("something"));
        Assertions.assertThat(t.isSeenBefore(loanId)).isFalse();
        final Optional<Investment> result = Investor.actuallyInvest(r, api, t);
        Assertions.assertThat(t.isSeenBefore(loanId)).isTrue();
        Assertions.assertThat(result).isEmpty();
        // validate event
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentDelegatedEvent.class);
        });
        // check that seen information is persisted
        final InvestmentTracker t2 = new InvestmentTracker(availableLoans, BigDecimal.valueOf(10000));
        Assertions.assertThat(t2.isSeenBefore(loanId)).isTrue();
    }

    @Test
    public void investmentSuccessful() {
        final BigDecimal oldBalance = BigDecimal.valueOf(10000);
        final int amountToInvest = 200;
        final InvestmentTracker t = new InvestmentTracker(Collections.emptyList(), oldBalance);
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(amountToInvest).get();
        final ZonkyProxy api = Mockito.mock(ZonkyProxy.class);
        Mockito.when(api.invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean()))
                .thenReturn(new ZonkyResponse(amountToInvest));
        final Optional<Investment> result = Investor.actuallyInvest(r, api, t);
        Assertions.assertThat(result).isPresent();
        final Investment investment = result.get();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(investment.getAmount()).isEqualTo(r.getRecommendedInvestmentAmount());
            softly.assertThat(investment.getLoanId()).isEqualTo(r.getLoanDescriptor().getLoan().getId());
        });
        // validate event sequence
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentMadeEvent.class);
        });
        // validate event contents
        final InvestmentMadeEvent e = (InvestmentMadeEvent)newEvents.get(1);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(e.getFinalBalance())
                    .isEqualTo(oldBalance.subtract(BigDecimal.valueOf(amountToInvest)).intValue());
            softly.assertThat(e.getInvestment()).isEqualTo(investment);
        });
    }

}
