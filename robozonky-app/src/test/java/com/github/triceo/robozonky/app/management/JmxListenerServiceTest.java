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

package com.github.triceo.robozonky.app.management;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.triceo.robozonky.api.notifications.SessionInfo;
import com.github.triceo.robozonky.api.notifications.StrategyCompletedEvent;
import com.github.triceo.robozonky.api.notifications.StrategyStartedEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JmxListenerServiceTest {

    private static final String USERNAME = "someone@somewhere.cz";
    private static final Supplier<Delinquency> DELINQUENCY = () -> (Delinquency) MBean.DELINQUENCY.getImplementation();
    private static final Supplier<InvestmentsMBean> INVESTMENTS =
            () -> (Investments) MBean.INVESTMENTS.getImplementation();
    private static final Supplier<RuntimeMBean> RUNTIME = () -> (Runtime) MBean.RUNTIME.getImplementation();
    private static final Supplier<Portfolio> PORTFOLIO = () -> (Portfolio) MBean.PORTFOLIO.getImplementation();

    private static Object[] getParametersForExecutionCompleted() {
        final ExecutionCompletedEvent evt = new ExecutionCompletedEvent(Collections.emptyList(), 0);
        final Consumer<SoftAssertions> before = (softly) -> {
            final InvestmentsMBean mbean = INVESTMENTS.get();
            final RuntimeMBean mbean2 = RUNTIME.get();
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isNull();
            softly.assertThat(mbean2.getZonkyUsername()).isEqualTo("");
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final InvestmentsMBean mbean = INVESTMENTS.get();
            final RuntimeMBean mbean2 = RUNTIME.get();
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
            softly.assertThat(mbean2.getZonkyUsername()).isEqualTo(USERNAME);
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentDelegated() {
        final Loan l = new Loan(1, 1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final Recommendation r = ld.recommend(200).get();
        final Event evt = new InvestmentDelegatedEvent(r, 10000, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            final InvestmentsMBean mbean = INVESTMENTS.get();
            softly.assertThat(mbean.getDelegatedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final InvestmentsMBean mbean = INVESTMENTS.get();
            softly.assertThat(mbean.getDelegatedInvestments()).containsOnlyKeys(l.getId());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentRejected() {
        final Loan l = new Loan(1, 1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final Recommendation r = ld.recommend(200).get();
        final Event evt = new InvestmentRejectedEvent(r, 10000, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            final InvestmentsMBean mbean = INVESTMENTS.get();
            softly.assertThat(mbean.getRejectedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final InvestmentsMBean mbean = INVESTMENTS.get();
            softly.assertThat(mbean.getRejectedInvestments()).containsOnlyKeys(l.getId());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForDelinquentLoan() {
        final Loan l = new Loan(1, 1000);
        final OffsetDateTime now = OffsetDateTime.now();
        final Event evt = new LoanNowDelinquentEvent(l, now.toLocalDate());
        final Consumer<SoftAssertions> before = (softly) -> {
            final DelinquencyMBean mbean = DELINQUENCY.get();
            softly.assertThat(mbean.getAll()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final DelinquencyMBean mbean = DELINQUENCY.get();
            softly.assertThat(mbean.getAll()).containsEntry(l.getId(), now.toLocalDate());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isAfterOrEqualTo(now);
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForNoLongerDelinquentLoan() {
        final OffsetDateTime now = OffsetDateTime.now();
        final Loan l = new Loan(1, 1000);
        final Event evt = new LoanNoLongerDelinquentEvent(l);
        final Consumer<SoftAssertions> before = (softly) -> {
            final DelinquencyMBean mbean = DELINQUENCY.get();
            softly.assertThat(mbean.getAll()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final DelinquencyMBean mbean = DELINQUENCY.get();
            softly.assertThat(mbean.getAll()).isEmpty();
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isAfterOrEqualTo(now);
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentMade() {
        final Loan l = new Loan(1, 1000);
        final Event evt = new InvestmentMadeEvent(new Investment(l, 200), 1000, false);
        final Consumer<SoftAssertions> before = (softly) -> {
            final InvestmentsMBean mbean = INVESTMENTS.get();
            softly.assertThat(mbean.getSuccessfulInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final InvestmentsMBean mbean = INVESTMENTS.get();
            softly.assertThat(mbean.getSuccessfulInvestments()).containsOnlyKeys(l.getId());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForStrategyStarted() {
        final StrategyStartedEvent evt = new StrategyStartedEvent(null, Collections.emptyList(), null);
        final Consumer<SoftAssertions> before = (softly) -> {
            final PortfolioMBean mbean = PORTFOLIO.get();
            softly.assertThat(mbean.getAvailableBalance()).isEqualTo(0);
            softly.assertThat(mbean.getExpectedYield()).isEqualTo(0);
            softly.assertThat(mbean.getInvestedAmount()).isEqualTo(0);
            softly.assertThat(mbean.getRelativeExpectedYield()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(mbean.getInvestedAmountPerRating()).isNotEmpty();
            softly.assertThat(mbean.getRatingShare()).isNotEmpty();
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isNull();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final PortfolioMBean mbean = PORTFOLIO.get();
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForStrategyCompleted() {
        final StrategyCompletedEvent evt = new StrategyCompletedEvent(null, Collections.emptyList(), null);
        final Consumer<SoftAssertions> before = (softly) -> {
            final PortfolioMBean mbean = PORTFOLIO.get();
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isNull();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final PortfolioMBean mbean = PORTFOLIO.get();
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(JmxListenerServiceTest.getParametersForExecutionCompleted(),
                             JmxListenerServiceTest.getParametersForInvestmentDelegated(),
                             JmxListenerServiceTest.getParametersForInvestmentMade(),
                             JmxListenerServiceTest.getParametersForInvestmentRejected(),
                             JmxListenerServiceTest.getParametersForStrategyStarted(),
                             JmxListenerServiceTest.getParametersForStrategyCompleted(),
                             JmxListenerServiceTest.getParametersForDelinquentLoan(),
                             JmxListenerServiceTest.getParametersForNoLongerDelinquentLoan());
    }

    /**
     * This exists so that parameterized tests can have a nicely readable ID. Don't put anything more complicated
     * there, as PIT mutation testing will silently ignore the test if the name of the test is weird.
     */
    @Parameterized.Parameter
    public Class<? extends Event> eventType;
    @Parameterized.Parameter(1)
    public Event event;
    @Parameterized.Parameter(2)
    public Consumer<SoftAssertions> assertionsBefore;
    @Parameterized.Parameter(3)
    public Consumer<SoftAssertions> assertionsAfter;

    public <T extends Event> void handleEvent(final T event) {
        final JmxListenerService service = new JmxListenerService();
        final Refreshable<EventListener<T>> r = service.findListener((Class<T>) event.getClass());
        r.run();
        final EventListener<T> listener = r.getLatest().get();
        listener.handle(event, new SessionInfo(USERNAME));
    }

    @Test
    public void set() {
        SoftAssertions.assertSoftly(assertionsBefore);
        this.handleEvent(event);
        SoftAssertions.assertSoftly(assertionsAfter);
    }

    @Test
    public void setInvalid() {
        final JmxListenerService service = new JmxListenerService();
        final Refreshable<EventListener<RoboZonkyEndingEvent>> r = service.findListener(RoboZonkyEndingEvent.class);
        r.run();
        Assertions.assertThat(r.getLatest()).isEmpty();
    }

    @After
    @Before
    public void reset() {
        Stream.of(MBean.values()).forEach(MBean::reset);
    }
}
