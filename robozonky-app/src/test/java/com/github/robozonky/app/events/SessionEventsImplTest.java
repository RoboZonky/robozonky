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

package com.github.robozonky.app.events;

import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.app.AbstractEventLeveragingTest;
import com.github.robozonky.app.events.impl.EventFactory;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SessionEventsImplTest extends AbstractEventLeveragingTest {

    @Test
    void identifiesEventTypeWhenClass() {
        final LoanDelinquent90DaysOrMoreEvent e = EventFactory.loanDelinquent90plus(Investment.custom().build(),
                                                                                    Loan.custom().build(),
                                                                                    LocalDate.now(),
                                                                                    Collections.emptyList());
        assertThat(SessionEvents.getImplementingEvent(e.getClass()))
                .isEqualTo(LoanDelinquent90DaysOrMoreEvent.class);
    }

    @Test
    void identifiesEventTypeWhenInterface() {
        assertThat(SessionEvents.getImplementingEvent(LoanDelinquent90DaysOrMoreEvent.class))
                .isEqualTo(LoanDelinquent90DaysOrMoreEvent.class);
    }

    @Test
    void registersListeners() {
        final EventFiringListener e = mock(EventFiringListener.class);
        assertThat(Events.forSession(SESSION).addListener(e)).isTrue();
        assertThat(Events.forSession(SESSION).addListener(e)).isFalse();
        assertThat(Events.forSession(SESSION).removeListener(e)).isTrue();
        assertThat(Events.forSession(SESSION).removeListener(e)).isFalse();
        assertThat(Events.forSession(SESSION).addListener(e)).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void callsListeners() {
        final ExecutionCompletedEvent s =
                EventFactory.executionCompleted(Collections.emptyList(), mockPortfolioOverview(10_000));
        final SessionEvents events = Events.forSession(SESSION);
        final EventFiringListener e = mock(EventFiringListener.class);
        final EventListener<ExecutionCompletedEvent> l = mock(EventListener.class);
        events.addListener(e);
        events.injectEventListener(l);
        events.fire(s);
        verify(e).requested(any());
        verify(e).ready(s, (Class<EventListener<ExecutionCompletedEvent>>)l.getClass());
        verify(e).fired(s, (Class<EventListener<ExecutionCompletedEvent>>)l.getClass());
        verify(l).handle(s, SESSION);
    }

    @SuppressWarnings("unchecked")
    @Test
    void callsListenersOnError() {
        final ExecutionCompletedEvent s =
                EventFactory.executionCompleted(Collections.emptyList(), mockPortfolioOverview(10_000));
        final SessionEvents events = Events.forSession(SESSION);
        final EventListener<ExecutionCompletedEvent> l = mock(EventListener.class);
        doThrow(IllegalStateException.class).when(l).handle(any(), any());
        events.injectEventListener(l);
        events.fire(s);
        assertThat(this.getEventsFailed()).isNotEmpty();
    }

    @Test
    void differentInstancesForDifferentUsernames() {
        final SessionEvents a = Events.forSession(SESSION);
        final SessionEvents b = Events.forSession(SESSION_DRY);
        assertThat(a).isSameAs(b);
        final SessionEvents c = Events.forSession(new SessionInfo(UUID.randomUUID().toString()));
        assertThat(a).isNotSameAs(c);
    }
}
