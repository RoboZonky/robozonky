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
import com.github.triceo.robozonky.api.strategies.Recommendation;
import com.github.triceo.robozonky.common.remote.AuthenticatedZonky;
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
        final AuthenticatedZonky zonky = AbstractInvestingTest.harmlessZonky(10_000);
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor();
        final Collection<LoanDescriptor> lds = Collections.singleton(ld);
        try (final Session it = Session.create(new Investor.Builder().build(zonky), zonky, lds)) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(it.getAvailableLoans())
                        .isNotSameAs(lds)
                        .containsExactly(ld);
                softly.assertThat(it.getInvestmentsMade()).isEmpty();
            });
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
        final AuthenticatedZonky zonky = AbstractInvestingTest.harmlessZonky(10_000);
        // test that the loan is not available
        try (final Session it = Session.create(new Investor.Builder().build(zonky), zonky, lds)) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(it.getAvailableLoans())
                        .hasSize(1)
                        .doesNotContain(ld);
                softly.assertThat(it.getInvestmentsMade()).isEmpty();
            });
        }
    }

    @Test
    public void registeringPreexistingInvestments() {
        final int loanId = 1;
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor(loanId);
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractInvestingTest.mockLoanDescriptor());
        // setup APIs
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final BlockedAmount ba = new BlockedAmount(loanId, Defaults.MINIMUM_INVESTMENT_IN_CZK);
        Mockito.when(z.getBlockedAmounts()).thenReturn(Stream.of(ba));
        Mockito.when(z.getLoan(ArgumentMatchers.eq(loanId))).thenReturn(ld.getLoan());
        try (final Session it = Session.create(new Investor.Builder().build(z), z, lds)) {
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(it.getAvailableLoans())
                        .hasSize(1)
                        .doesNotContain(ld);
                softly.assertThat(it.getInvestmentsMade()).isEmpty();
            });
        }
    }

    @Test
    public void makeInvestment() {
        // setup APIs
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(10_000);
        // run test
        final int amount = 200;
        final LoanDescriptor ld = AbstractInvestingTest.mockLoanDescriptor(Duration.ZERO);
        final Collection<LoanDescriptor> lds = Arrays.asList(ld, AbstractInvestingTest.mockLoanDescriptor());
        try (final Session it = Session.create(new Investor.Builder().build(z), z, lds)) {
            it.invest(ld.recommend(amount).get());
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(it.getAvailableLoans()).isNotEmpty().doesNotContain(ld);
                softly.assertThat(it.getInvestmentsMade()).hasSize(1);
                softly.assertAll();
            });
        }
    }

    @Test
    public void getBalancePropertyInDryRun() {
        final int value = 200;
        System.setProperty("robozonky.default.dry_run_balance", String.valueOf(value));
        final Investor.Builder p = new Investor.Builder().asDryRun();
        Assertions.assertThat(Session.getAvailableBalance(p.build(null), null).intValue()).isEqualTo(value);
    }

    @Test
    public void getBalancePropertyIgnoredWhenNotDryRun() {
        System.setProperty("robozonky.default.dry_run_balance", "200");
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(0);
        final Investor.Builder p = new Investor.Builder();
        Assertions.assertThat(Session.getAvailableBalance(p.build(z), z)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void getRemoteBalanceInDryRun() {
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(0);
        final Investor.Builder p = new Investor.Builder().asDryRun();
        Assertions.assertThat(Session.getAvailableBalance(p.build(z), z)).isEqualTo(BigDecimal.ZERO);
    }

    @Test
    public void underBalance() {
        // setup APIs
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(0);
        // run test
        final Investor p = new Investor.Builder().build(z);
        try (final Session it = Session.create(p, z, Collections.emptyList())) {
            final Optional<Recommendation> recommendation =
                    AbstractInvestingTest.mockLoanDescriptor().recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK);
            final boolean result = it.invest(recommendation.get());
            // verify result
            Assertions.assertThat(result).isFalse();
        }
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).isEmpty();
    }

    @Test
    public void underAmount() {
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(0);
        final Investor p = new Investor.Builder().build(z);
        final Recommendation recommendation =
                AbstractInvestingTest.mockLoanDescriptor().recommend(Defaults.MINIMUM_INVESTMENT_IN_CZK).get();
        try (final Session t = Session.create(p, z, Collections.singletonList(recommendation.getLoanDescriptor()))) {
            final boolean result = t.invest(recommendation);
            // verify result
            Assertions.assertThat(result).isFalse();
        }
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).isEmpty();
    }

    @Test
    public void investmentFailed() {
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor p = Mockito.spy(new Investor.Builder().build(z));
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        final Exception thrown = new ServiceUnavailableException();
        Mockito.doThrow(thrown).when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        try (final Session t = Session.create(p, z, Collections.emptyList())) {
            Assertions.assertThatThrownBy(() -> t.invest(r)).isSameAs(thrown);
        }
    }

    @Test
    public void investmentRejected() {
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor p = Mockito.spy(new Investor.Builder().build(z));
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(200).get();
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        try (final Session t = Session.create(p, z, Collections.emptyList())) {
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
        final Recommendation r = ld.recommend(200).get();
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor p = Mockito.spy(new Investor.Builder().build(z));
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        try (final Session t = Session.create(p, z, availableLoans)) {
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
        final Recommendation r = ld.recommend(200, true).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor p = Mockito.spy(new Investor.Builder().build(z));
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.DELEGATED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.of("something")).when(p).getConfirmationProviderId();
        try (final Session t = Session.create(p, z, availableLoans)) {
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
        final Recommendation r = ld.recommend(200).get();
        final Collection<LoanDescriptor> availableLoans = Collections.singletonList(ld);
        // setup APIs
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor p = Mockito.spy(new Investor.Builder().build(z));
        Mockito.doReturn(new ZonkyResponse(ZonkyResponseType.REJECTED))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.empty()).when(p).getConfirmationProviderId();
        try (final Session t = Session.create(p, z, availableLoans)) {
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
        final Recommendation r = AbstractInvestingTest.mockLoanDescriptor().recommend(amountToInvest).get();
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(oldBalance);
        final Investor p = Mockito.spy(new Investor.Builder().build(z));
        Mockito.doReturn(new ZonkyResponse(amountToInvest))
                .when(p).invest(ArgumentMatchers.eq(r), ArgumentMatchers.anyBoolean());
        Mockito.doReturn(Optional.empty()).when(p).getConfirmationProviderId();
        try (final Session t = Session.create(p, z, Collections.emptyList())) {
            final boolean result = t.invest(r);
            Assertions.assertThat(result).isTrue();
            final List<Investment> investments = t.getInvestmentsMade();
            Assertions.assertThat(investments).hasSize(1);
            Assertions.assertThat(investments.get(0).getAmount()).isEqualTo(amountToInvest);
        }
        // validate event sequence
        final List<Event> newEvents = this.getNewEvents();
        Assertions.assertThat(newEvents).hasSize(2);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(newEvents.get(0)).isInstanceOf(InvestmentRequestedEvent.class);
            softly.assertThat(newEvents.get(1)).isInstanceOf(InvestmentMadeEvent.class);
        });
        // validate event contents
        final InvestmentMadeEvent e = (InvestmentMadeEvent)newEvents.get(1);
        Assertions.assertThat(e.getFinalBalance())
                .isEqualTo(oldBalance - amountToInvest);
    }

    @Test
    public void exclusivity() {
        final AuthenticatedZonky z = AbstractInvestingTest.harmlessZonky(10_000);
        final Investor p = Mockito.spy(new Investor.Builder().build(z));
        try (final Session t = Session.create(p, z, Collections.emptyList())) {
            Assertions.assertThat(t).isNotNull();
            // verify second session fails
            Assertions.assertThatThrownBy(() -> Session.create(p, z, Collections.emptyList()))
                    .isExactlyInstanceOf(IllegalStateException.class);
        }
        // and verify sessions are properly closed
        try (final Session t2 = Session.create(p, AbstractInvestingTest.harmlessZonky(0), Collections.emptyList())) {
            Assertions.assertThat(t2).isNotNull();
        }
    }

}
