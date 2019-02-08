/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SecondaryMarketplaceAccessorTest extends AbstractZonkyLeveragingTest {

    @Test
    void hasAdditions() {
        final long[] original = new long[]{1};
        final long[] updated = new long[]{1, 2};
        assertSoftly(softly -> {
            softly.assertThat(SecondaryMarketplaceAccessor.hasAdditions(new long[0], original)).isFalse();
            softly.assertThat(SecondaryMarketplaceAccessor.hasAdditions(updated, original)).isTrue();
            softly.assertThat(SecondaryMarketplaceAccessor.hasAdditions(updated, original)).isTrue();
            softly.assertThat(SecondaryMarketplaceAccessor.hasAdditions(original, original)).isFalse();
        });
    }

    @Test
    void readsMarketplace() {
        final Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(1l);
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getAvailableParticipations(any())).thenReturn(Stream.of(p));
        final Tenant tenant = mockTenant(zonky);
        final MarketplaceAccessor<ParticipationDescriptor> d = new SecondaryMarketplaceAccessor(tenant,
                                                                                                UnaryOperator.identity(),
                                                                                                f -> f.item().getId());
        final Collection<ParticipationDescriptor> ld = d.getMarketplace();
        assertThat(ld).hasSize(1)
                .element(0)
                .extracting(ParticipationDescriptor::item)
                .isSameAs(p);
    }

    @Test
    void hasUpdatesWhenCurrentAndPreviousEmpty() {
        final Participation p = mock(Participation.class);
        when(p.getId()).thenReturn(1l);
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getAvailableParticipations(any())).thenReturn(Stream.of(p));
        final Tenant tenant = mockTenant(zonky);
        final AtomicReference<long[]> state = new AtomicReference<>(new long[0]);
        final MarketplaceAccessor<ParticipationDescriptor> a = new SecondaryMarketplaceAccessor(tenant,
                                                                                                state::getAndSet,
                                                                                                f -> f.item().getId());
        assertThat(a.hasUpdates()).isTrue(); // detect update, store present state
        assertThat(a.hasUpdates()).isFalse(); // state same as marketplace, no update
    }
}
