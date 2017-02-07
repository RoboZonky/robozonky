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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyInitializedEvent;
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

    private static final Runtime BEAN = (Runtime)MBean.RUNTIME.getImplementation();

    private static Object[] getParametersForExecutionStarted() {
        final ExecutionStartedEvent evt = new ExecutionStartedEvent("user", Collections.emptyList(), 0);
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(BEAN.getLastInvestmentRunTimestamp()).isBefore(evt.getCreatedOn());
            softly.assertThat(BEAN.getZonkyUsername()).isEqualTo("");
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(BEAN.getLastInvestmentRunTimestamp()).isEqualTo(evt.getCreatedOn());
            softly.assertThat(BEAN.getZonkyUsername()).isEqualTo(evt.getUsername());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentDelegated() {
        final Loan l = new Loan(1, 1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final Recommendation r = ld.recommend(200).get();
        final Event evt = new InvestmentDelegatedEvent(r, 10000, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(BEAN.getDelegatedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(BEAN.getDelegatedInvestments()).containsOnlyKeys(l.getId());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentRejected() {
        final Loan l = new Loan(1, 1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final Recommendation r = ld.recommend(200).get();
        final Event evt = new InvestmentRejectedEvent(r, 10000, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(BEAN.getRejectedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(BEAN.getRejectedInvestments()).containsOnlyKeys(l.getId());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentMade() {
        final Loan l = new Loan(1, 1000);
        final Event evt = new InvestmentMadeEvent(new Investment(l, 200), 1000);
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(BEAN.getSuccessfulInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(BEAN.getSuccessfulInvestments()).containsOnlyKeys(l.getId());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForRoboZonkyInitialized() {
        final RoboZonkyInitializedEvent evt = new RoboZonkyInitializedEvent("version");
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(BEAN.getVersion()).isEqualTo("");
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(BEAN.getVersion()).isEqualTo(evt.getVersion());
        };
        return new Object[] {evt.getClass(), evt, before, after};
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(JmxListenerServiceTest.getParametersForExecutionStarted(),
                JmxListenerServiceTest.getParametersForInvestmentDelegated(),
                JmxListenerServiceTest.getParametersForInvestmentMade(),
                JmxListenerServiceTest.getParametersForInvestmentRejected(),
                JmxListenerServiceTest.getParametersForRoboZonkyInitialized());
    }

    @Before
    public void clearBean() {
        BEAN.clear();
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
        listener.handle(event);
    }

    @Test
    public void set() {
        SoftAssertions.assertSoftly(assertionsBefore);
        this.handleEvent(event);
        SoftAssertions.assertSoftly(assertionsAfter);
        BEAN.clear();
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
