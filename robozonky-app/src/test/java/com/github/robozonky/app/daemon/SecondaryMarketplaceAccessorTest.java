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

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.LastPublishedParticipation;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecondaryMarketplaceAccessorTest extends AbstractZonkyLeveragingTest {

    @Test
    void readsMarketplace() {
        final Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(1l);
        when(p.getLoanHealthInfo()).thenReturn(LoanHealth.HEALTHY);
        final Zonky zonky = harmlessZonky();
        when(zonky.getAvailableParticipations(any())).thenReturn(Stream.of(p));
        final PowerTenant tenant = mockTenant(zonky);
        final MarketplaceAccessor<ParticipationDescriptor> d =
                new SecondaryMarketplaceAccessor(tenant, UnaryOperator.identity());
        final Collection<ParticipationDescriptor> ld = d.getMarketplace();
        assertThat(ld).hasSize(1)
                .element(0)
                .extracting(ParticipationDescriptor::item)
                .isSameAs(p);
    }

    @Test
    void detectsUpdates() {
        final Zonky z = harmlessZonky();
        when(z.getLastPublishedParticipationInfo()).thenReturn(mock(LastPublishedParticipation.class));
        final PowerTenant t = mockTenant(z);
        final AtomicReference<LastPublishedParticipation> state = new AtomicReference<>(null);
        final MarketplaceAccessor<ParticipationDescriptor> a =
                new SecondaryMarketplaceAccessor(t, state::getAndSet);
        assertThat(a.hasUpdates()).isTrue(); // detect update, store present state
        assertThat(a.hasUpdates()).isFalse(); // state stays the same, no update
    }

    @Test
    void failsDetection() {
        final Zonky z = harmlessZonky();
        when(z.getLastPublishedParticipationInfo()).thenThrow(IllegalStateException.class);
        final PowerTenant t = mockTenant(z);
        final AtomicReference<LastPublishedParticipation> state = new AtomicReference<>(null);
        final MarketplaceAccessor<ParticipationDescriptor> a =
                new SecondaryMarketplaceAccessor(t, state::getAndSet);
        assertThat(a.hasUpdates()).isTrue();
        assertThat(a.hasUpdates()).isTrue();
    }
}
