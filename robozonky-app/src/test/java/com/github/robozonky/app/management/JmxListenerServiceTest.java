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
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.robozonky.api.ReturnCode;
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
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.Mockito.*;

class JmxListenerServiceTest extends AbstractRoboZonkyTest {

    private static final String USERNAME = "someone@somewhere.cz";
    private static Optional<Consumer<ShutdownHook.Result>> MGMT;

    private static com.github.robozonky.app.management.Runtime getRuntimeMBean() {
        return (com.github.robozonky.app.management.Runtime) JmxListenerService.getMBean(MBean.RUNTIME)
                .orElseThrow(() -> new IllegalStateException("Not found"));
    }

    private static Operations getOperationsMBean() {
        return (Operations) JmxListenerService.getMBean(MBean.OPERATIONS)
                .orElseThrow(() -> new IllegalStateException("Not found"));
    }

    private static Portfolio getPortfolioMBean() {
        return (Portfolio) JmxListenerService.getMBean(MBean.PORTFOLIO)
                .orElseThrow(() -> new IllegalStateException("Not found"));
    }

    @BeforeAll
    static void loadAll() {
        MGMT = new Management(mock(Lifecycle.class)).get();
    }

    @AfterAll
    static void unloadAll() {
        MGMT.ifPresent(rc -> rc.accept(new ShutdownHook.Result(ReturnCode.OK, null)));
    }

    private DynamicTest getParametersForExecutionCompleted() {
        final ExecutionCompletedEvent evt = new ExecutionCompletedEvent(Collections.emptyList(), null);
        final Consumer<SoftAssertions> before = (softly) -> {
            softly.assertThat(getRuntimeMBean().getZonkyUsername()).isEqualTo("");
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(getRuntimeMBean().getZonkyUsername()).isEqualTo(USERNAME);
            softly.assertThat(getRuntimeMBean().getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
            softly.assertThat(getPortfolioMBean().getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return getParameters(evt, before, after);
    }

    private DynamicTest getParameters(final Event evt) {
        final Consumer<SoftAssertions> before = (softly) -> {
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            softly.assertThat(getPortfolioMBean().getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return getParameters(evt, before, after);
    }

    private DynamicTest getParametersForSellingStarted() {
        return getParameters(new SellingStartedEvent(Collections.emptyList(), null));
    }

    private DynamicTest getParametersForSellingCompleted() {
        return getParameters(new SellingCompletedEvent(Collections.emptyList(), null));
    }

    private DynamicTest getParametersForPurchasingStarted() {
        return getParameters(new PurchasingStartedEvent(Collections.emptyList(), null));
    }

    private DynamicTest getParametersForPurchasingCompleted() {
        return getParameters(new PurchasingCompletedEvent(Collections.emptyList(), null));
    }

    private DynamicTest getParameters(final Event evt, final Consumer<SoftAssertions> before,
                                      final Consumer<SoftAssertions> after) {
        return dynamicTest(evt.getClass().getSimpleName(), () -> testSet(evt, before, after));
    }

    private static Loan mockLoan() {
        return Loan.custom()
                .setId(1)
                .setAmount(1000)
                .setRemainingInvestment(1000)
                .build();
    }

    private DynamicTest getParametersForInvestmentDelegated() {
        final Loan l = mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(l);
        final RecommendedLoan r = ld.recommend(200).get();
        final Event evt = new InvestmentDelegatedEvent(r, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getDelegatedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getDelegatedInvestments()).containsOnlyKeys(l.getId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return getParameters(evt, before, after);
    }

    private DynamicTest getParametersForInvestmentRejected() {
        final Loan l = mockLoan();
        final LoanDescriptor ld = new LoanDescriptor(l);
        final RecommendedLoan r = ld.recommend(200).get();
        final Event evt = new InvestmentRejectedEvent(r, "");
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getRejectedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getRejectedInvestments()).containsOnlyKeys(l.getId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return getParameters(evt, before, after);
    }

    private DynamicTest getParametersForInvestmentMade() {
        final Loan l = mockLoan();
        final Investment i = Investment.fresh((MarketplaceLoan) l, 200);
        final PortfolioOverview po = PortfolioOverview.calculate(BigDecimal.valueOf(1000), Stream::empty);
        final Event evt = new InvestmentMadeEvent(i, l, po);
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getSuccessfulInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getSuccessfulInvestments()).containsOnlyKeys(l.getId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return getParameters(evt, before, after);
    }

    private DynamicTest getParametersForSaleOffered() {
        final Loan l = mockLoan();
        final Investment i = Investment.fresh((MarketplaceLoan) l, 200);
        final Event evt = new SaleOfferedEvent(i, l);
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getOfferedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getOfferedInvestments()).containsOnlyKeys(i.getLoanId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return getParameters(evt, before, after);
    }

    private DynamicTest getParametersForInvestmentPurchased() {
        final Loan l = mockLoan();
        final Investment i = Investment.fresh((MarketplaceLoan) l, 200);
        final Event evt = new InvestmentPurchasedEvent(i, l, PortfolioOverview.calculate(BigDecimal.valueOf(2000),
                                                                                         Stream::empty));
        final Consumer<SoftAssertions> before = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getPurchasedInvestments()).isEmpty();
        };
        final Consumer<SoftAssertions> after = (softly) -> {
            final OperationsMBean mbean = getOperationsMBean();
            softly.assertThat(mbean.getPurchasedInvestments()).containsOnlyKeys(i.getLoanId());
            softly.assertThat(mbean.getLatestUpdatedDateTime()).isEqualTo(evt.getCreatedOn());
        };
        return getParameters(evt, before, after);
    }

    private void testSet(final Event event, final Consumer<SoftAssertions> assertionsBefore,
                         final Consumer<SoftAssertions> assertionsAfter) {
        assertSoftly(assertionsBefore);
        handleEvent(event);
        assertSoftly(assertionsAfter);
    }

    @TestFactory
    Stream<DynamicTest> events() {
        return Stream.of(
                getParametersForExecutionCompleted(),
                getParametersForPurchasingStarted(),
                getParametersForPurchasingCompleted(),
                getParametersForSellingStarted(),
                getParametersForSellingCompleted(),
                getParametersForInvestmentDelegated(),
                getParametersForInvestmentMade(),
                getParametersForInvestmentRejected(),
                getParametersForSaleOffered(),
                getParametersForInvestmentPurchased());
    }

    private <T extends Event> void handleEvent(final T event) {
        final JmxListenerService service = new JmxListenerService();
        final EventListenerSupplier<T> r = service.findListener((Class<T>) event.getClass());
        final EventListener<T> listener = r.get().get();
        listener.handle(event, new SessionInfo(USERNAME));
    }

    @Test
    void setInvalid() {
        final JmxListenerService service = new JmxListenerService();
        final EventListenerSupplier<RoboZonkyEndingEvent> r = service.findListener(RoboZonkyEndingEvent.class);
        assertThat(r).isNull();
    }
}
