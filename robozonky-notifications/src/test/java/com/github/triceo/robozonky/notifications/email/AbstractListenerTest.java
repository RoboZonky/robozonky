/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.email;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentDelegatedEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentMadeEvent;
import com.github.triceo.robozonky.api.notifications.InvestmentRejectedEvent;
import com.github.triceo.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.api.strategies.Recommendation;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public abstract class AbstractListenerTest {

    private static NotificationProperties getNotificationProperties() {
        System.setProperty(NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY,
                NotificationPropertiesTest.class.getResource("notifications-enabled.cfg").toString());
        final Optional<NotificationProperties> p = NotificationProperties.getProperties();
        System.clearProperty(NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY);
        return p.get();
    }

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getListeners() {
        // prepare data
        final Loan loan = Mockito.mock(Loan.class);
        Mockito.when(loan.getDatePublished()).thenReturn(OffsetDateTime.now());
        Mockito.when(loan.getRemainingInvestment()).thenReturn(1000.0);
        Mockito.when(loan.getAmount()).thenReturn(100000.0);
        final LoanDescriptor loanDescriptor = new LoanDescriptor(loan);
        final Recommendation recommendation = loanDescriptor.recommend(200, false).get();
        final Investment i = new Investment(loan, 200);
        final NotificationProperties properties = AbstractListenerTest.getNotificationProperties();
        // create events for listeners
        final Map<Class<? extends Event>, Event> events = new HashMap<>(SupportedListener.values().length);
        events.put(InvestmentDelegatedEvent.class, new InvestmentDelegatedEvent(recommendation, 200, "random"));
        events.put(InvestmentMadeEvent.class, new InvestmentMadeEvent(i, 200));
        events.put(InvestmentRejectedEvent.class, new InvestmentRejectedEvent(recommendation, 200, "random"));
        events.put(ExecutionStartedEvent.class, new ExecutionStartedEvent(Collections.emptyList(), 200));
        events.put(RoboZonkyCrashedEvent.class, new RoboZonkyCrashedEvent(ReturnCode.ERROR_UNEXPECTED, null));
        // create the listeners
        return Stream.of(SupportedListener.values())
                .map(s -> new Object[] {s.getEventType(), s.getListener(properties), events.get(s.getEventType())})
                .collect(Collectors.toList());
    }

    @Parameterized.Parameter(1)
    public EventListener<Event> listener;
    @Parameterized.Parameter(2)
    public Event event;
    // only exists so that the parameter can have a nice constant description. otherwise PIT will report 0 coverage.
    @Parameterized.Parameter
    public Class<? extends Event> eventType;

}
