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

package com.github.robozonky.app.daemon;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.LastPublishedParticipation;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.test.mock.MockLoanBuilder;

class SecondaryMarketplaceAccessorTest extends AbstractZonkyLeveragingTest {

    @Test
    void readsMarketplace() {
        final Loan l = new MockLoanBuilder().build();
        int loanId = l.getId();
        final Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(1l);
        when(p.getLoanId()).thenReturn(loanId);
        when(p.getLoanHealthInfo()).thenReturn(LoanHealth.HEALTHY);
        final Zonky zonky = harmlessZonky();
        when(zonky.getLoan(eq(loanId))).thenReturn(l);
        when(zonky.getAvailableParticipations(any())).thenReturn(Stream.of(p));
        final PowerTenant tenant = mockTenant(zonky);
        final AbstractMarketplaceAccessor<ParticipationDescriptor> d = new SecondaryMarketplaceAccessor(tenant,
                UnaryOperator.identity());
        final Collection<ParticipationDescriptor> pd = d.getMarketplace();
        assertThat(pd).hasSize(1)
            .element(0)
            .extracting(ParticipationDescriptor::item)
            .isSameAs(p);
        assertThat(pd)
            .element(0)
            .extracting(ParticipationDescriptor::related)
            .isSameAs(l);
    }

    @Test
    void detectsUpdates() {
        final Zonky z = harmlessZonky();
        when(z.getLastPublishedParticipationInfo()).thenReturn(mock(LastPublishedParticipation.class));
        final PowerTenant t = mockTenant(z);
        final AtomicReference<LastPublishedParticipation> state = new AtomicReference<>(null);
        final AbstractMarketplaceAccessor<ParticipationDescriptor> a = new SecondaryMarketplaceAccessor(t,
                state::getAndSet);
        assertThat(a.hasUpdates()).isTrue(); // detect update, store present state
        assertThat(a.hasUpdates()).isFalse(); // state stays the same, no update
    }

    @Test
    void failsDetection() {
        final Zonky z = harmlessZonky();
        when(z.getLastPublishedParticipationInfo()).thenThrow(IllegalStateException.class);
        final PowerTenant t = mockTenant(z);
        final AtomicReference<LastPublishedParticipation> state = new AtomicReference<>(null);
        final AbstractMarketplaceAccessor<ParticipationDescriptor> a = new SecondaryMarketplaceAccessor(t,
                state::getAndSet);
        assertThat(a.hasUpdates()).isTrue();
        assertThat(a.hasUpdates()).isTrue();
    }

    @Test
    void incrementality() {
        final Zonky z = harmlessZonky();
        when(z.getLastPublishedLoanInfo()).thenThrow(IllegalStateException.class);
        final PowerTenant t = mockTenant(z);
        DateUtil.setSystemClock(Clock.fixed(Instant.EPOCH, Defaults.ZONE_ID));
        final AbstractMarketplaceAccessor<ParticipationDescriptor> a = new SecondaryMarketplaceAccessor(t, null);
        Select initial = a.getIncrementalFilter();
        assertThat(initial).isNotNull();
        DateUtil.setSystemClock(Clock.fixed(Instant.now(), Defaults.ZONE_ID));
        Select anotherFull = a.getIncrementalFilter();
        assertThat(anotherFull)
            .isNotNull()
            .isNotEqualTo(initial);
        DateUtil.setSystemClock(Clock.fixed(Instant.now()
            .plusSeconds(1), Defaults.ZONE_ID));
        Select incremental = a.getIncrementalFilter();
        assertThat(incremental)
            .isNotNull()
            .isNotEqualTo(initial);
    }
}
