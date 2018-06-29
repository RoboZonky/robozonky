/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.notifications.listeners;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.robozonky.api.notifications.InvestmentPurchasedEvent;
import com.github.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.robozonky.api.notifications.InvestmentSkippedEvent;
import com.github.robozonky.api.notifications.InvestmentSoldEvent;
import com.github.robozonky.api.notifications.LoanDefaultedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent10DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent30DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent60DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.notifications.LoanLostEvent;
import com.github.robozonky.api.notifications.LoanNoLongerDelinquentEvent;
import com.github.robozonky.api.notifications.LoanNowDelinquentEvent;
import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.notifications.RemoteOperationFailedEvent;
import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyExperimentalUpdateDetectedEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import com.github.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;
import com.github.robozonky.api.notifications.SaleOfferedEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.notifications.AbstractTargetHandler;
import com.github.robozonky.notifications.ConfigStorage;
import com.github.robozonky.notifications.NotificationListenerService;
import com.github.robozonky.notifications.SupportedListener;
import com.github.robozonky.notifications.Target;
import com.github.robozonky.notifications.templates.TemplateProcessor;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import freemarker.template.TemplateException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DynamicContainer;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.notNull;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AbstractListenerTest extends AbstractRoboZonkyTest {

    private static final RoboZonkyTestingEvent EVENT = new RoboZonkyTestingEvent();
    private static final PortfolioOverview MAX_PORTFOLIO = mockPortfolio(Integer.MAX_VALUE);
    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.net");

    private static PortfolioOverview mockPortfolio(final int balance) {
        final PortfolioOverview portfolioOverview = mock(PortfolioOverview.class);
        when(portfolioOverview.getCzkAvailable()).thenReturn(balance);
        when(portfolioOverview.getCzkAtRisk()).thenReturn(0);
        when(portfolioOverview.getShareAtRisk()).thenReturn(BigDecimal.ZERO);
        when(portfolioOverview.getCzkAtRisk(any())).thenReturn(0);
        when(portfolioOverview.getShareOnInvestment(any())).thenReturn(BigDecimal.ZERO);
        when(portfolioOverview.getAtRiskShareOnInvestment(any())).thenReturn(BigDecimal.ZERO);
        return portfolioOverview;
    }

    private static AbstractListener<? extends Event> getListener(final SupportedListener s,
                                                                 final AbstractTargetHandler p) {
        final AbstractListener<? extends Event> e = spy((AbstractListener<? extends Event>) s.getListener(p));
        // always return a listener that WILL send an e-mail, even though this means shouldSendEmail() is not tested
        doReturn(true).when(e).shouldNotify(any(), eq(SESSION_INFO));
        return e;
    }

    private static <T extends Event> void testFormal(final AbstractListener<T> listener, final T event,
                                                     final SupportedListener listenerType) {
        assertThat(event).isInstanceOf(listenerType.getEventType());
        assertThat(listener.getTemplateFileName())
                .isNotNull()
                .isNotEmpty();
    }

    private static <T extends Event> void testProcessing(final AbstractListener<T> listener,
                                                         final T event) throws IOException, TemplateException {
        final String s = TemplateProcessor.INSTANCE.process(listener.getTemplateFileName(),
                                                            listener.getData(event, SESSION_INFO));
        assertThat(s).contains(Defaults.ROBOZONKY_URL);
    }

    private static void testListenerEnabled(final Event event) {
        final NotificationListenerService service = new NotificationListenerService();
        final Stream<? extends EventListenerSupplier<? extends Event>> supplier =
                service.findListeners(event.getClass());
        assertThat(supplier).isNotEmpty();
    }

    static AbstractTargetHandler getHandler(final ConfigStorage storage) {
        return spy(new TestingTargetHandler(storage));
    }

    static AbstractTargetHandler getHandler() throws IOException {
        final ConfigStorage cs =
                ConfigStorage.create(AbstractListenerTest.class.getResourceAsStream("notifications-enabled.cfg"));
        return getHandler(cs);
    }

    private static <T extends Event> void testTriggered(final AbstractTargetHandler h,
                                                        final AbstractListener<T> listener,
                                                        final T event) throws Exception {
        BalanceTracker.reset(SESSION_INFO);
        listener.handle(event, SESSION_INFO);
        verify(h, times(1)).actuallySend(notNull(), notNull(), notNull());
    }

    @SuppressWarnings("unchecked")
    private static <T extends Event> DynamicContainer forListener(final SupportedListener listener,
                                                                  final T e) throws IOException {
        final AbstractTargetHandler p = getHandler();
        final AbstractListener<T> l = (AbstractListener<T>) getListener(listener, p);
        return DynamicContainer.dynamicContainer(listener.toString(), Stream.of(
                dynamicTest("is formally correct", () -> testFormal(l, e, listener)),
                dynamicTest("is processed correctly", () -> testProcessing(l, e)),
                dynamicTest("triggers the sending code", () -> testTriggered(p, l, e)),
                dynamicTest("has listener enabled", () -> testListenerEnabled(e))
        ));
    }

    @BeforeEach
    void configureNotifications() throws URISyntaxException, MalformedURLException {
        ListenerServiceLoader.registerNotificationConfiguration(SESSION_INFO,
                                                                AbstractListenerTest.class.getResource(
                                                                        "notifications-enabled.cfg").toURI().toURL());
    }

    @Test
    void spamProtectionAvailable() throws Exception {
        final ConfigStorage cs =
                ConfigStorage.create(getClass().getResourceAsStream("notifications-enabled-spamless.cfg"));
        final AbstractTargetHandler p = getHandler(cs);
        final TestingEmailingListener l = new TestingEmailingListener(p);
        l.handle(EVENT, SESSION_INFO);
        verify(p, times(1)).actuallySend(notNull(), notNull(), notNull());
        l.handle(EVENT, SESSION_INFO);
        // e-mail not re-sent, finisher not called again
        verify(p, times(1)).actuallySend(notNull(), notNull(), notNull());
    }

    @BeforeEach
    @AfterEach
    @Override
    protected void deleteState() { // JUnit 5 won't invoke this from an abstract class
        super.deleteState();
    }

    @TestFactory
    Stream<DynamicNode> listeners() throws IOException {
        // prepare data
        final Loan loan = Loan.custom()
                .setId(66666)
                .setAmount(100_000)
                .setInterestRate(BigDecimal.TEN)
                .setDatePublished(OffsetDateTime.now().minusMonths(2))
                .setName("Úvěr")
                .setRegion(Region.JIHOCESKY)
                .setPurpose(Purpose.AUTO_MOTO)
                .setMainIncomeType(MainIncomeType.EMPLOYMENT)
                .setRemainingInvestment(2000)
                .setRating(Rating.AAAAA)
                .setTermInMonths(25)
                .setUrl(new URL("http://www.robozonky.cz"))
                .build();
        final LoanDescriptor loanDescriptor = new LoanDescriptor(loan);
        final RecommendedLoan recommendation = loanDescriptor.recommend(1200, false).get();
        final Investment i = Investment.fresh((MarketplaceLoan) loan, 1000)
                .setInvestmentDate(OffsetDateTime.now())
                .build();
        // create events for listeners
        return Stream.of(
                forListener(SupportedListener.INVESTMENT_DELEGATED,
                            new InvestmentDelegatedEvent(recommendation, "random")),
                forListener(SupportedListener.INVESTMENT_MADE, new InvestmentMadeEvent(i, loan, MAX_PORTFOLIO)),
                forListener(SupportedListener.INVESTMENT_SOLD, new InvestmentSoldEvent(i, loan, MAX_PORTFOLIO)),
                forListener(SupportedListener.INVESTMENT_SKIPPED, new InvestmentSkippedEvent(recommendation)),
                forListener(SupportedListener.INVESTMENT_REJECTED,
                            new InvestmentRejectedEvent(recommendation, "random")),
                forListener(SupportedListener.LOAN_NO_LONGER_DELINQUENT,
                            new LoanNoLongerDelinquentEvent(i, loan)),
                forListener(SupportedListener.LOAN_DEFAULTED,
                            new LoanDefaultedEvent(i, loan)),
                forListener(SupportedListener.LOAN_LOST,
                            new LoanLostEvent(i, loan)),
                forListener(SupportedListener.LOAN_NOW_DELINQUENT,
                            new LoanNowDelinquentEvent(i, loan, LocalDate.now(), Collections.emptyList())),
                forListener(SupportedListener.LOAN_DELINQUENT_10_PLUS,
                            new LoanDelinquent10DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(11),
                                                                Collections.emptyList())),
                forListener(SupportedListener.LOAN_DELINQUENT_30_PLUS,
                            new LoanDelinquent30DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(31),
                                                                Collections.emptyList())),
                forListener(SupportedListener.LOAN_DELINQUENT_60_PLUS,
                            new LoanDelinquent60DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(61),
                                                                Collections.emptyList())),
                forListener(SupportedListener.LOAN_DELINQUENT_90_PLUS,
                            new LoanDelinquent90DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(91),
                                                                Collections.emptyList())),
                forListener(SupportedListener.LOAN_REPAID, new LoanRepaidEvent(i, loan, MAX_PORTFOLIO)),
                forListener(SupportedListener.BALANCE_ON_TARGET,
                            new ExecutionStartedEvent(Collections.emptyList(), MAX_PORTFOLIO)),
                forListener(SupportedListener.BALANCE_UNDER_MINIMUM,
                            new ExecutionStartedEvent(Collections.emptyList(), mockPortfolio(0))),
                forListener(SupportedListener.CRASHED,
                            new RoboZonkyCrashedEvent(new RuntimeException())),
                forListener(SupportedListener.REMOTE_OPERATION_FAILED,
                            new RemoteOperationFailedEvent(new RuntimeException())),
                forListener(SupportedListener.DAEMON_FAILED, new RoboZonkyDaemonFailedEvent(new RuntimeException())),
                forListener(SupportedListener.INITIALIZED, new RoboZonkyInitializedEvent()),
                forListener(SupportedListener.ENDING, new RoboZonkyEndingEvent()),
                forListener(SupportedListener.TESTING, new RoboZonkyTestingEvent()),
                forListener(SupportedListener.UPDATE_DETECTED, new RoboZonkyUpdateDetectedEvent("1.2.3")),
                forListener(SupportedListener.EXPERIMENTAL_UPDATE_DETECTED,
                            new RoboZonkyExperimentalUpdateDetectedEvent("1.3.0-beta-1")),
                forListener(SupportedListener.INVESTMENT_PURCHASED,
                            new InvestmentPurchasedEvent(i, loan, MAX_PORTFOLIO)),
                forListener(SupportedListener.SALE_OFFERED, new SaleOfferedEvent(i, loan))
        );
    }

    private static class TestingTargetHandler extends AbstractTargetHandler {

        public TestingTargetHandler(final ConfigStorage storage) {
            super(storage, Target.EMAIL);
        }

        @Override
        public void actuallySend(final SessionInfo sessionInfo, final String subject, final String message) {

        }
    }

    private static final class TestingEmailingListener extends AbstractListener<RoboZonkyTestingEvent> {

        TestingEmailingListener(final AbstractTargetHandler handler) {
            super(SupportedListener.TESTING, handler);
        }

        @Override
        String getSubject(final RoboZonkyTestingEvent event) {
            return "No actual subject";
        }

        @Override
        String getTemplateFileName() {
            return "testing.ftl";
        }
    }
}
