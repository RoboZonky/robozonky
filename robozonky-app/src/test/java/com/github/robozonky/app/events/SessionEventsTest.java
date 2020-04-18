/*
 * Copyright 2020 The RoboZonky Project
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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.mockito.Mockito.*;

import java.time.Duration;
import java.time.LocalDate;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.LoanDelinquent90DaysOrMoreEvent;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.SessionInfoImpl;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.test.mock.MockInvestmentBuilder;
import com.github.robozonky.test.mock.MockLoanBuilder;

class SessionEventsTest extends AbstractEventLeveragingTest {

    private final Zonky zonky = harmlessZonky();
    private final PowerTenant tenant = mockTenant(zonky, false);
    private final PowerTenant tenantDry = mockTenant(zonky, true);

    @Test
    void lazyFireReturnsFuture() {
        final Loan l = MockLoanBuilder.fresh();
        final Investment i = MockInvestmentBuilder.fresh(l, 200)
            .build();
        final CompletableFuture<?> result = SessionEvents.forSession(mockSessionInfo())
            .fire(EventFactory.loanNoLongerDelinquentLazy(() -> EventFactory.loanNoLongerDelinquent(i, l)));
        result.join(); // make sure it does not throw
        assertThat(getEventsRequested()).hasSize(1);
    }

    @Test
    void fireReturnsFuture() {
        final Loan l = MockLoanBuilder.fresh();
        final Investment i = MockInvestmentBuilder.fresh(l, 200)
            .build();
        final CompletableFuture<?> result = SessionEvents.forSession(mockSessionInfo())
            .fire(EventFactory.loanNoLongerDelinquent(i, l));
        result.join(); // make sure it does not throw
        assertThat(getEventsRequested()).hasSize(1);
    }

    @Test
    void identifiesEventTypeWhenClass() {
        final LoanDelinquent90DaysOrMoreEvent e = EventFactory.loanDelinquent90plus(MockInvestmentBuilder.fresh()
            .build(),
                MockLoanBuilder.fresh(), LocalDate.now());
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
        assertThat(Events.forSession(tenant)
            .addListener(e)).isTrue();
        assertThat(Events.forSession(tenant)
            .addListener(e)).isFalse();
        assertThat(Events.forSession(tenant)
            .removeListener(e)).isTrue();
        assertThat(Events.forSession(tenant)
            .removeListener(e)).isFalse();
        assertThat(Events.forSession(tenant)
            .addListener(e)).isTrue();
    }

    @SuppressWarnings("unchecked")
    @Test
    void callsListeners() {
        final ExecutionCompletedEvent s = EventFactory.executionCompleted(Collections.emptyList(),
                mockPortfolioOverview());
        final SessionEvents events = Events.forSession(tenant);
        final EventFiringListener e = mock(EventFiringListener.class);
        final EventListener<ExecutionCompletedEvent> l = mock(EventListener.class);
        events.addListener(e);
        events.injectEventListener(l);
        final CompletableFuture<?> r = events.fire(s);
        assertTimeoutPreemptively(Duration.ofSeconds(5), r::join);
        assertThat(getEventsRequested()).isNotEmpty();
        assertThat(getEventsReady()).isNotEmpty();
        assertThat(getEventsFired()).isNotEmpty();
        verify(l).handle(s, tenant.getSessionInfo());
    }

    @SuppressWarnings("unchecked")
    @Test
    void callsListenersOnError() {
        final ExecutionCompletedEvent s = EventFactory.executionCompleted(Collections.emptyList(),
                mockPortfolioOverview());
        final SessionEvents events = Events.forSession(tenant);
        final EventListener<ExecutionCompletedEvent> l = mock(EventListener.class);
        doThrow(IllegalStateException.class).when(l)
            .handle(any(), any());
        events.injectEventListener(l);
        final CompletableFuture<?> r = events.fire(s);
        assertTimeoutPreemptively(Duration.ofSeconds(5), r::join);
        assertThat(this.getEventsFailed()).isNotEmpty();
    }

    @Test
    void differentInstancesForDifferentUsernames() {
        final SessionEvents a = Events.forSession(tenant);
        assertThat(a.getSessionInfo()
            .getUsername()).isEqualTo(tenant.getSessionInfo()
                .getUsername());
        final SessionEvents b = Events.forSession(tenantDry);
        assertThat(a.getSessionInfo()
            .getUsername()).isEqualTo(tenantDry.getSessionInfo()
                .getUsername());
        assertThat(a)
            .isNotNull()
            .isSameAs(b);
        final PowerTenant t3 = mockTenant();
        when(t3.getSessionInfo()).thenReturn(new SessionInfoImpl(UUID.randomUUID()
            .toString()));
        final SessionEvents c = Events.forSession(t3);
        assertThat(c.getSessionInfo()).isEqualTo(t3.getSessionInfo());
        assertThat(a)
            .isNotNull()
            .isNotSameAs(c);
    }
}
