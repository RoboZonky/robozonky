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
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JmxListenerServiceTest {

    private static final String USERNAME = "someone@somewhere.cz";
    private static final InvestmentsMBean INVESTMENTS = (Investments)MBean.INVESTMENTS.getImplementation();
    private static final RuntimeMBean RUNTIME = (Runtime)MBean.RUNTIME.getImplementation();
    private static final Portfolio PORTFOLIO = (Portfolio) MBean.PORTFOLIO.getImplementation();

    private static Object[] getParametersForExecutionCompleted() {
        final ExecutionCompletedEvent evt = new ExecutionCompletedEvent(Collections.emptyList(), 0);
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(INVESTMENTS.getLatestUpdatedDateTime()).isNull();
            softly.assertThat(RUNTIME.getZonkyUsername()).isEqualTo("");
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(INVESTMENTS.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
            softly.assertThat(RUNTIME.getZonkyUsername()).isEqualTo(USERNAME);
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentDelegated() {
        final Loan l = new Loan(1, 1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final Recommendation r = ld.recommend(200).get();
        final Event evt = new InvestmentDelegatedEvent(r, 10000, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(INVESTMENTS.getDelegatedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(INVESTMENTS.getDelegatedInvestments()).containsOnlyKeys(l.getId());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentRejected() {
        final Loan l = new Loan(1, 1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final Recommendation r = ld.recommend(200).get();
        final Event evt = new InvestmentRejectedEvent(r, 10000, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(INVESTMENTS.getRejectedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(INVESTMENTS.getRejectedInvestments()).containsOnlyKeys(l.getId());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentMade() {
        final Loan l = new Loan(1, 1000);
        final Event evt = new InvestmentMadeEvent(new Investment(l, 200), 1000, false);
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(INVESTMENTS.getSuccessfulInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(INVESTMENTS.getSuccessfulInvestments()).containsOnlyKeys(l.getId());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForStrategyStarted() {
        final StrategyStartedEvent evt = new StrategyStartedEvent(null, Collections.emptyList(), null);
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(PORTFOLIO.getAvailableBalance()).isEqualTo(0);
            softly.assertThat(PORTFOLIO.getExpectedYield()).isEqualTo(0);
            softly.assertThat(PORTFOLIO.getInvestedAmount()).isEqualTo(0);
            softly.assertThat(PORTFOLIO.getRelativeExpectedYield()).isEqualTo(BigDecimal.ZERO);
            softly.assertThat(PORTFOLIO.getInvestedAmountPerRating()).isNotEmpty();
            softly.assertThat(PORTFOLIO.getRatingShare()).isNotEmpty();
            softly.assertThat(PORTFOLIO.getLatestUpdatedDateTime()).isNull();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(PORTFOLIO.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForStrategyCompleted() {
        final StrategyCompletedEvent evt = new StrategyCompletedEvent(null, Collections.emptyList(), null);
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(PORTFOLIO.getLatestUpdatedDateTime()).isNull();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(PORTFOLIO.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(JmxListenerServiceTest.getParametersForExecutionCompleted(),
                JmxListenerServiceTest.getParametersForInvestmentDelegated(),
                JmxListenerServiceTest.getParametersForInvestmentMade(),
                JmxListenerServiceTest.getParametersForInvestmentRejected(),
                JmxListenerServiceTest.getParametersForStrategyStarted(),
                JmxListenerServiceTest.getParametersForStrategyCompleted());
    }

    @Before
    public void clearBean() {
        Stream.of(MBean.values()).forEach(b -> b.getImplementation().reset());
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
        final Refreshable<EventListener<T>> r = service.findListener((Class<T>)event.getClass());
        r.run();
        final EventListener<T> listener = r.getLatest().get();
        listener.handle(event, new SessionInfo(USERNAME));
    }

    @Test
    public void set() {
        SoftAssertions.assertSoftly(assertionsBefore);
        this.handleEvent(event);
        SoftAssertions.assertSoftly(assertionsAfter);
        Stream.of(MBean.values()).forEach(b -> b.getImplementation().reset());
        SoftAssertions.assertSoftly(assertionsBefore);
    }

    @Test
    public void setInvalid() {
        final JmxListenerService service = new JmxListenerService();
        final Refreshable<EventListener<RoboZonkyEndingEvent>> r = service.findListener(RoboZonkyEndingEvent.class);
        r.run();
        Assertions.assertThat(r.getLatest()).isEmpty();
    }

}
