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

package com.github.robozonky.app.management;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.PurchasingCompletedEvent;
import com.github.robozonky.api.notifications.PurchasingStartedEvent;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.notifications.SellingStartedEvent;
import com.github.robozonky.api.notifications.SessionInfo;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JmxListenerServiceTest extends AbstractRoboZonkyTest {

    private static final String USERNAME = "someone@somewhere.cz";
    private static final Supplier<OperationsMBean> OPERATIONS = () -> (Operations) MBean.OPERATIONS.getImplementation();
    private static final Supplier<RuntimeMBean> RUNTIME = () -> (Runtime) MBean.RUNTIME.getImplementation();
    private static final Supplier<PortfolioMBean> PORTFOLIO = () -> (Portfolio) MBean.PORTFOLIO.getImplementation();
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

    private static Object[] getParametersForExecutionCompleted() {
        final ExecutionCompletedEvent evt = new ExecutionCompletedEvent(Collections.emptyList(), null);
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(RUNTIME.get().getZonkyUsername()).isEqualTo("");
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(RUNTIME.get().getZonkyUsername()).isEqualTo(USERNAME);
            softly.assertThat(RUNTIME.get().getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
            softly.assertThat(PORTFOLIO.get().getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParameters(final Event evt) {
        final Consumer<SoftAssertions> before = (softly) -> {
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(PORTFOLIO.get().getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForSellingStarted() {
        return getParameters(new SellingStartedEvent(Collections.emptyList(), null));
    }

    private static Object[] getParametersForSellingCompleted() {
        return getParameters(new SellingCompletedEvent(Collections.emptyList(), null));
    }

    private static Object[] getParametersForPurchasingStarted() {
        return getParameters(new PurchasingStartedEvent(Collections.emptyList(), null));
    }

    private static Object[] getParametersForPurchasingCompleted() {
        return getParameters(new PurchasingCompletedEvent(Collections.emptyList(), null));
    }

    private static Object[] getParametersForInvestmentDelegated() {
        final Loan l = new Loan(1, 1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final RecommendedLoan r = ld.recommend(200).get();
        final Event evt = new InvestmentDelegatedEvent(r, 10000, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getDelegatedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getDelegatedInvestments()).containsOnlyKeys(l.getId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentRejected() {
        final Loan l = new Loan(1, 1000);
        final LoanDescriptor ld = new LoanDescriptor(l);
        final RecommendedLoan r = ld.recommend(200).get();
        final Event evt = new InvestmentRejectedEvent(r, 10000, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getRejectedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getRejectedInvestments()).containsOnlyKeys(l.getId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentMade() {
        final Loan l = new Loan(1, 1000);
        final PortfolioOverview po = PortfolioOverview.calculate(BigDecimal.valueOf(1000), Collections.emptyList());
        final Event evt = new InvestmentMadeEvent(new Investment(l, 200), po, false);
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getSuccessfulInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getSuccessfulInvestments()).containsOnlyKeys(l.getId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForSaleOffered() {
        final Investment l = new Investment(new Loan(1, 1000), 200);
        final Event evt = new SaleOfferedEvent(l, true);
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getOfferedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getOfferedInvestments()).containsOnlyKeys(l.getLoanId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    private static Object[] getParametersForInvestmentPurchased() {
        final Investment l = new Investment(new Loan(1, 1000), 200);
        final Event evt = new InvestmentPurchasedEvent(l, PortfolioOverview.calculate(BigDecimal.valueOf(2000),
                                                                                      Collections.emptyList()),
                                                       true);
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getPurchasedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = OPERATIONS.get();
            softly.assertThat(mbean.getPurchasedInvestments()).containsOnlyKeys(l.getLoanId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return new Object[]{evt.getClass(), evt, before, after};
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(JmxListenerServiceTest.getParametersForExecutionCompleted(),
                             JmxListenerServiceTest.getParametersForPurchasingStarted(),
                             JmxListenerServiceTest.getParametersForPurchasingCompleted(),
                             JmxListenerServiceTest.getParametersForSellingStarted(),
                             JmxListenerServiceTest.getParametersForSellingCompleted(),
                             JmxListenerServiceTest.getParametersForInvestmentDelegated(),
                             JmxListenerServiceTest.getParametersForInvestmentMade(),
                             JmxListenerServiceTest.getParametersForInvestmentRejected(),
                             JmxListenerServiceTest.getParametersForSaleOffered(),
                             JmxListenerServiceTest.getParametersForInvestmentPurchased());
    }

    public <T extends Event> void handleEvent(final T event) {
        final JmxListenerService service = new JmxListenerService();
        final EventListenerSupplier<T> r = service.findListener((Class<T>) event.getClass());
        final EventListener<T> listener = r.get().get();
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
        final EventListenerSupplier<RoboZonkyEndingEvent> r = service.findListener(RoboZonkyEndingEvent.class);
        Assertions.assertThat(r).isNull();
    }

    @After
    @Before
    public void reset() {
        Stream.of(MBean.values()).forEach(MBean::reset);
    }
}
