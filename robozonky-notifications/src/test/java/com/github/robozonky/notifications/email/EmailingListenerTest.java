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

package com.github.robozonky.notifications.email;

import java.io.IOException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.mail.internet.MimeMessage;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
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
import com.github.robozonky.api.notifications.SessionInfo;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.State;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import com.github.robozonky.util.Refreshable;
import com.icegreen.greenmail.util.GreenMail;
import com.icegreen.greenmail.util.ServerSetup;
import com.icegreen.greenmail.util.ServerSetupTest;
import freemarker.template.TemplateException;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.slf4j.LoggerFactory;

@RunWith(Parameterized.class)
public class EmailingListenerTest extends AbstractRoboZonkyTest {

    private static final RoboZonkyTestingEvent EVENT = new RoboZonkyTestingEvent();

    private static final GreenMail EMAIL = new GreenMail(getServerSetup());
    @Rule
    public final ProvideSystemProperty myPropertyHasMyValue = new ProvideSystemProperty(
            RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
            NotificationPropertiesTest.class.getResource("notifications-enabled.cfg").toString());
    @Parameterized.Parameter(1)
    public AbstractEmailingListener<Event> listener;
    // only exists so that the parameter can have a nice constant description. otherwise PIT will report 0 coverage.
    @Parameterized.Parameter(2)
    public Event event;
    @Parameterized.Parameter
    public SupportedListener listenerType;

    @BeforeClass
    public static void startEmailing() {
        EMAIL.start();
    }

    @AfterClass
    public static void stopEmailing() {
        try {
            EMAIL.stop();
        } catch (final Exception ex) {
            LoggerFactory.getLogger(EmailingListenerTest.class).warn("Failed stopping e-mail server.", ex);
        }
    }

    private static ServerSetup getServerSetup() {
        final ServerSetup setup = ServerSetupTest.SMTP;
        setup.setServerStartupTimeout(5000);
        setup.setVerbose(true);
        return setup;
    }

    private static NotificationProperties getNotificationProperties() {
        System.setProperty(RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
                           NotificationPropertiesTest.class.getResource("notifications-enabled.cfg").toString());
        final Refreshable<NotificationProperties> r = new RefreshableNotificationProperties();
        r.run();
        final Optional<NotificationProperties> p = r.get();
        System.clearProperty(RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY);
        return p.get();
    }

    private static PortfolioOverview mockPortfolio(final int balance) {
        final PortfolioOverview portfolioOverview = Mockito.mock(PortfolioOverview.class);
        Mockito.when(portfolioOverview.getCzkAvailable()).thenReturn(balance);
        return portfolioOverview;
    }

    private static EventListener<Event> getListener(final SupportedListener s, final NotificationProperties p) {
        final AbstractEmailingListener<Event> e = Mockito.spy((AbstractEmailingListener<Event>) s.getListener(p));
        // always return a listener that WILL send an e-mail, even though this means shouldSendEmail() is not tested
        Mockito.doReturn(true).when(e).shouldSendEmail(ArgumentMatchers.any());
        return e;
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getListeners() {
        // prepare data
        final Loan loan = Mockito.spy(new Loan(66666, 100000));
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now().minusMonths(2));
        Mockito.when(loan.getName()).thenReturn("Úvěr");
        Mockito.when(loan.getRegion()).thenReturn(Region.JIHOCESKY);
        Mockito.when(loan.getPurpose()).thenReturn(Purpose.AUTO_MOTO);
        Mockito.when(loan.getMainIncomeType()).thenReturn(MainIncomeType.EMPLOYMENT);
        Mockito.when(loan.getRemainingInvestment()).thenReturn(2000.0);
        Mockito.when(loan.getRating()).thenReturn(Rating.AAAAA);
        Mockito.when(loan.getTermInMonths()).thenReturn(25);
        Mockito.when(loan.getUrl()).thenReturn("http://www.robozonky.cz/");
        final LoanDescriptor loanDescriptor = new LoanDescriptor(loan);
        final RecommendedLoan recommendation = loanDescriptor.recommend(1200, false).get();
        final Investment i = new Investment(loan, 1000);
        final NotificationProperties properties = EmailingListenerTest.getNotificationProperties();
        // create events for listeners
        final Map<SupportedListener, Event> events = new HashMap<>(SupportedListener.values().length);
        events.put(SupportedListener.INVESTMENT_DELEGATED, new InvestmentDelegatedEvent(recommendation, "random"));
        events.put(SupportedListener.INVESTMENT_MADE,
                   new InvestmentMadeEvent(i, loan, mockPortfolio(Integer.MAX_VALUE)));
        events.put(SupportedListener.INVESTMENT_SOLD, new InvestmentSoldEvent(i, loan,
                                                                              mockPortfolio(Integer.MAX_VALUE)));
        events.put(SupportedListener.INVESTMENT_SKIPPED, new InvestmentSkippedEvent(recommendation));
        events.put(SupportedListener.INVESTMENT_REJECTED, new InvestmentRejectedEvent(recommendation, "random"));
        events.put(SupportedListener.LOAN_NO_LONGER_DELINQUENT,
                   new LoanNoLongerDelinquentEvent(i, loan, LocalDate.now()));
        events.put(SupportedListener.LOAN_DEFAULTED, new LoanDefaultedEvent(i, loan, LocalDate.now()));
        events.put(SupportedListener.LOAN_NOW_DELINQUENT, new LoanNowDelinquentEvent(i, loan, LocalDate.now()));
        events.put(SupportedListener.LOAN_DELINQUENT_10_PLUS,
                   new LoanDelinquent10DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(11)));
        events.put(SupportedListener.LOAN_DELINQUENT_30_PLUS,
                   new LoanDelinquent30DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(31)));
        events.put(SupportedListener.LOAN_DELINQUENT_60_PLUS,
                   new LoanDelinquent60DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(61)));
        events.put(SupportedListener.LOAN_DELINQUENT_90_PLUS,
                   new LoanDelinquent90DaysOrMoreEvent(i, loan, LocalDate.now().minusDays(91)));
        events.put(SupportedListener.LOAN_REPAID, new LoanRepaidEvent(i, loan, mockPortfolio(Integer.MAX_VALUE)));
        events.put(SupportedListener.BALANCE_ON_TARGET,
                   new ExecutionStartedEvent(Collections.emptyList(), mockPortfolio(Integer.MAX_VALUE)));
        events.put(SupportedListener.BALANCE_UNDER_MINIMUM,
                   new ExecutionStartedEvent(Collections.emptyList(), mockPortfolio(0)));
        events.put(SupportedListener.CRASHED,
                   new RoboZonkyCrashedEvent(ReturnCode.ERROR_UNEXPECTED, new RuntimeException()));
        events.put(SupportedListener.REMOTE_OPERATION_FAILED, new RemoteOperationFailedEvent(new RuntimeException()));
        events.put(SupportedListener.DAEMON_FAILED, new RoboZonkyDaemonFailedEvent(new RuntimeException()));
        events.put(SupportedListener.INITIALIZED, new RoboZonkyInitializedEvent());
        events.put(SupportedListener.ENDING, new RoboZonkyEndingEvent());
        events.put(SupportedListener.TESTING, new RoboZonkyTestingEvent());
        events.put(SupportedListener.UPDATE_DETECTED, new RoboZonkyUpdateDetectedEvent("1.2.3"));
        events.put(SupportedListener.EXPERIMENTAL_UPDATE_DETECTED,
                   new RoboZonkyExperimentalUpdateDetectedEvent("1.3.0-beta-1"));
        events.put(SupportedListener.INVESTMENT_PURCHASED,
                   new InvestmentPurchasedEvent(i, loan, mockPortfolio(Integer.MAX_VALUE)));
        events.put(SupportedListener.SALE_OFFERED, new SaleOfferedEvent(i, loan));
        // create the listeners
        return Stream.of(SupportedListener.values())
                .map(s -> new Object[]{s, getListener(s, properties), events.get(s)})
                .collect(Collectors.toList());
    }

    @Before
    @After
    public void resetEmailing() throws Exception {
        EMAIL.purgeEmailFromAllMailboxes();
    }

    @After
    public void resetBalanceTracker() { // to make sure the tests always return consistent results
        BalanceTracker.INSTANCE.reset();
    }

    @After
    public void resetDelinquencyTracker() { // to make sure the tests always return consistent results
        State.forClass(DelinquencyTracker.class).newBatch(true).call();
    }

    @Test
    public void testMailSent() throws Exception {
        final AbstractEmailingListener<Event> l = this.listener;
        l.handle(this.event, new SessionInfo("someone@somewhere.net"));
        Assertions.assertThat(EMAIL.getReceivedMessages()).hasSize(1);
        final MimeMessage m = EMAIL.getReceivedMessages()[0];
        Assertions.assertThat(m.getContentType()).contains(Defaults.CHARSET.displayName());
        Assertions.assertThat(m.getSubject()).isNotNull().isEqualTo(l.getSubject(this.event));
        Assertions.assertThat(m.getFrom()[0].toString()).contains("user@seznam.cz");
    }

    @Test
    public void formal() {
        Assertions.assertThat(this.event).isInstanceOf(this.listenerType.getEventType());
        final AbstractEmailingListener<Event> l = this.listener;
        Assertions.assertThat(l.getTemplateFileName())
                .isNotNull()
                .isNotEmpty();
    }

    @Test
    public void processingWithoutErrors() throws IOException, TemplateException {
        final String s = TemplateProcessor.INSTANCE.process(this.listener.getTemplateFileName(),
                                                            this.listener.getData(event, new SessionInfo(
                                                                    "someone@somewhere.net")));
        Assertions.assertThat(s).contains(Defaults.ROBOZONKY_URL);
    }

    @Test
    public void reportingEnabledHaveListeners() {
        final EmailListenerService service = new EmailListenerService();
        final EventListenerSupplier<?> supplier = service.findListener(this.event.getClass());
        Assertions.assertThat(supplier.get()).isPresent();
    }

    @Test
    public void spamProtectionAvailable() throws IOException {
        final Properties props = new Properties();
        props.load(NotificationPropertiesTest.class.getResourceAsStream("notifications-enabled.cfg"));
        int sendCount = 1;
        props.setProperty("hourlyMaxEmails", String.valueOf(sendCount)); // spam protection
        final ListenerSpecificNotificationProperties p =
                new ListenerSpecificNotificationProperties(SupportedListener.TESTING,
                                                           new NotificationProperties(props));
        final Consumer<RoboZonkyTestingEvent> c = Mockito.mock(Consumer.class);
        final TestingEmailingListener l = new TestingEmailingListener(p);
        l.registerFinisher(c);
        Assertions.assertThat(l.countFinishers()).isEqualTo(2); // both spam protection and custom finisher available
        l.handle(EVENT, new SessionInfo("someone@somewhere.net"));
        Mockito.verify(c, Mockito.times(sendCount)).accept(ArgumentMatchers.any());
        Assertions.assertThat(EMAIL.getReceivedMessages()).hasSize(sendCount);
        l.handle(EVENT, new SessionInfo("someone@somewhere.net"));
        // e-mail not re-sent, finisher not called again
        Mockito.verify(c, Mockito.times(sendCount)).accept(ArgumentMatchers.any());
        Assertions.assertThat(EMAIL.getReceivedMessages()).hasSize(sendCount);
    }

    private static final class TestingEmailingListener extends AbstractEmailingListener<RoboZonkyTestingEvent> {

        public TestingEmailingListener(final ListenerSpecificNotificationProperties properties) {
            super(properties);
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

