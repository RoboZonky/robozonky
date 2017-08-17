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

package com.github.triceo.robozonky.app.investing;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import javax.ws.rs.ServiceUnavailableException;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRequestedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.triceo.robozonky.api.remote.entities.BlockedAmount;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.RecommendedLoan;
import com.github.triceo.robozonky.app.portfolio.Portfolio;
import com.github.triceo.robozonky.common.remote.Zonky;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.RestoreSystemProperties;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class SessionTest extends AbstractInvestingTest {

    @Rule
    public final RestoreSystemProperties propertyRestore = new RestoreSystemProperties();

    @Test
    public void constructor() {
        final Zonky zonky = AbstractInvestingTest.harmlessZonky(10_000);
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Collections.singleton(ld);
        try (final Session it = Session.create(new Investor.Builder(), zonky, lds)) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(it.getAvailable())
                        .isNotSameAs(lds)
                        .containsExactly(ld);
                softly.assertThat(it.getResult()).isEmpty();
            });
            // no new sessions should be allowed before the current one is disposed of
            Assertions.assertThatThrownBy(() -> Session.create(new Investor.Builder(), zonky, lds))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    public void discardingInvestments() {
        final int loanId = 1;
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor(loanId);
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractInvestingTest.mockLoanDescriptor());
        // discard the loan
        final SessionState sst = new SessionState(lds);
        sst.discard(ld);
        // setup APIs
        final Zonky zonky = AbstractInvestingTest.harmlessZonky(10_000);
        // test that the loan is not available
        try (final Session it = Session.create(new Investor.Builder(), zonky, lds)) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(it.getAvailable())
                        .hasSize(1)
                        .doesNotContain(ld);
                softly.assertThat(it.getResult()).isEmpty();
            });
        }
    }

    @Test
    public void registeringPreexistingInvestments() {
        final int loanId = 1;
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor(loanId);
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractInvestingTest.mockLoanDescriptor());
        // setup APIs
        final Zonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final BlockedAmount ba = new BlockedAmount(loanId, BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK));
        Mockito.when(z.getBlockedAmounts()).thenReturn(Stream.of(ba));
        Mockito.when(z.getLoan(ArgumentMatchers.eq(loanId))).thenReturn(ld.item());
        Portfolio.INSTANCE.update(z); // make sure data from API is loaded
        try (final Session it = Session.create(new Investor.Builder(), z, lds)) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(it.getAvailable()).hasSize(1).doesNotContain(ld);
                softly.assertThat(it.getResult()).isEmpty();
            });
        }
    }

    @Test
    public void makeInvestment() {
        // setup APIs
        final Zonky z = AbstractInvestingTest.harmlessZonky(200);
        // run test
        final int amount = 200;
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptorWithoutCaptcha();
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractInvestingTest.mockLoanDescriptor());
        try (final Session it = Session.create(new Investor.Builder(), z, lds)) {
            it.invest(ld.recommend(BigDecimal.valueOf(amount)).get());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(it.getAvailable()).isNotEmpty().doesNotContain(ld);
                softly.assertThat(it.getResult()).hasSize(1);
                softly.assertAll();
            });
        }
    }

    @Test
    public void underBalance() {
        // setup APIs
        final Zonky z = AbstractInvestingTest.harmlessZonky(Defaults.MINIMUM_INVESTMENT_IN_CZK - 1);
        // run test
        try (final Session it = Session.create(new Investor.Builder(), z, Collections.emptyList())) {
            final Optional<RecommendedLoan> recommendation = AbstractInvestingTest.mockLoanDescriptor()
                    .recommend(BigDecimal.valueOf(Defaults.MINIMUM_INVESTMENT_IN_CZK));
            final boolean result = it.invest(recommendation.get());
            // verify result
            Assertions.assertThat(result).isFalse();
        }
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).isEmpty();
    }

    @Test
    public void underAmount() {
        final Zonky z = AbstractInvestingTest.harmlessZonky(0);
        final RecommendedLoan recommendation =
                AbstractInvestingTest.mockLoanDescriptor().recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK).get();
        try (final Session t = Session.create(new Investor.Builder(), z,
                                              Collections.singletonList(recommendation.descriptor()))) {
            final boolean result = t.invest(recommendation);
            // verify result
            Assertions.assertThat(result).isFalse();
        }
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).isEmpty();
    }

    @Test
    public void investmentFailed() {
        final Zonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final RecommendedLoan r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        final Exception thrown = new ServiceUnavailableException();
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doThrow(thrown).when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        final Investor.Builder b = Mockito.spy(new Investor.Builder());
        Mockito.doReturn(p).when(b).build(ArgumentMatchers.eq(z));
        try (final Session t = Session.create(b, z, Collections.emptyList())) {
            Assertions.assertThatThrownBy(() -> t.invest(r)).isSameAs(thrown);
        }
    }

    @Test
    public void investmentRejected() {
        final Zonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final RecommendedLoan r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Investor.Builder b = Mockito.spy(new Investor.Builder());
        Mockito.doReturn(p).when(b).build(ArgumentMatchers.eq(z));
        try (final Session t = Session.create(b, z, Collections.emptyList())) {
            final boolean result = t.invest(r);
            Assertions.assertThat(result).isFalse();
        }
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
        final RecommendedLoan r = ld.recommend(200).get();
        final Zonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Investor.Builder b = Mockito.spy(new Investor.Builder());
        Mockito.doReturn(p).when(b).build(ArgumentMatchers.eq(z));
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        try (final Session t = Session.create(b, z, availableLoans)) {
            final boolean result = t.invest(r);
            Assertions.assertThat(result).isFalse();
        }
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
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200, true).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        final Zonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Investor.Builder b = Mockito.spy(new Investor.Builder());
        Mockito.doReturn(p).when(b).build(ArgumentMatchers.eq(z));
        try (final Session t = Session.create(b, z, availableLoans)) {
            final boolean result = t.invest(r);
            Assertions.assertThat(result).isFalse();
        }
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
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final RecommendedLoan r = ld.recommend(200).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        // setup APIs
        final Zonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.empty()).when(p).getConfirmationProviderId();
        final Investor.Builder b = Mockito.spy(new Investor.Builder());
        Mockito.doReturn(p).when(b).build(ArgumentMatchers.eq(z));
        try (final Session t = Session.create(b, z, availableLoans)) {
            final boolean result = t.invest(r);
            Assertions.assertThat(result).isFalse();
        }
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
        final RecommendedLoan r = AbstractInvestingTest.mockLoanDescriptor().recommend(amountToInvest).get();
        final Zonky z = AbstractInvestingTest.harmlessZonky(oldBalance);
        final Investor p = Mockito.mock(Investor.class);
        Mockito.doReturn(z).when(p).getZonky();
        Mockito.doReturn(new ZonkyResponse(amountToInvest))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Investor.Builder b = Mockito.spy(new Investor.Builder());
        Mockito.doReturn(p).when(b).build(ArgumentMatchers.eq(z));
        try (final Session t = Session.create(b, z, Collections.emptyList())) {
            final boolean result = t.invest(r);
            Assertions.assertThat(result).isTrue();
            final List<Investment> investments = t.getResult();
            Assertions.assertThat(investments).hasSize(1);
            Assertions.assertThat(investments.get(0).getAmount().intValue()).isEqualTo(amountToInvest);
        }
        // validate event sequence
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentMadeEvent.class);
        });
        // validate event contents
        final InvestmentMadeEvent e = (InvestmentMadeEvent) newEvents.get(1);
        Assertions.assertThat(e.getFinalBalance())
                .isEqualTo(oldBalance - amountToInvest);
    }

    @Test
    public void exclusivity() {
        final Zonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor.Builder b = new Investor.Builder();
        try (final Session t = Session.create(b, z, Collections.emptyList())) {
            Assertions.assertThat(t).isNotNull();
            // verify second session fails
            Assertions.assertThatThrownBy(() -> Session.create(b, z, Collections.emptyList()))
                    .isExactlyInstanceOf(IllegalStateException.class);
        }
        // and verify sessions are properly closed
        try (final Session t2 = Session.create(b, AbstractInvestingTest.harmlessZonky(0), Collections.emptyList())) {
            Assertions.assertThat(t2).isNotNull();
        }
    }
}
