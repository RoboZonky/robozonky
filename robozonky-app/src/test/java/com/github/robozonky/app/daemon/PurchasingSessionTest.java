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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.PurchaseRequestedEvent;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.RemotePortfolio;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

class PurchasingSessionTest extends AbstractZonkyLeveragingTest {

    @Test
    void empty() {
        final Zonky z = harmlessZonky(0);
        final PowerTenant auth = mockTenant(z);
        final Collection<Investment> i = PurchasingSession.purchase(auth, Collections.emptyList(), null);
        assertThat(i).isEmpty();
    }

    @Test
    void underBalance() {
        final Participation p = mock(Participation.class);
        when(p.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(200));
        final Loan l = Loan.custom().build();
        final PurchaseStrategy s = mock(PurchaseStrategy.class);
        when(s.recommend(any(), any(), any()))
                .thenAnswer(i -> {
                    final Collection<ParticipationDescriptor> participations = i.getArgument(0);
                    return participations.stream()
                            .map(ParticipationDescriptor::recommend)
                            .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
                });
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        final Zonky z = harmlessZonky(0);
        final PowerTenant auth = mockTenant(z);
        final Collection<Investment> i = PurchasingSession.purchase(auth, Collections.singleton(pd), s);
        assertSoftly(softly -> {
            softly.assertThat(i).isEmpty();
            softly.assertThat(getEventsRequested()).has(new Condition<List<? extends Event>>() {
                @Override
                public boolean matches(final List<? extends Event> events) {
                    return events.stream().noneMatch(e -> e instanceof PurchaseRequestedEvent);
                }
            });
        });
    }

    @Test
    void properReal() {
        final Loan l = Loan.custom()
                .setId(1)
                .setAmount(200)
                .setRating(Rating.D)
                .setNonReservedRemainingInvestment(200)
                .setMyInvestment(mockMyInvestment())
                .build();
        final Participation p = mock(Participation.class);
        when(p.getLoanId()).thenReturn(l.getId());
        when(p.getRemainingPrincipal()).thenReturn(BigDecimal.valueOf(200));
        final PurchaseStrategy s = mock(PurchaseStrategy.class);
        when(s.recommend(any(), any(), any())).thenAnswer(i -> {
            final Collection<ParticipationDescriptor> participations = i.getArgument(0);
            return participations.stream()
                    .map(ParticipationDescriptor::recommend)
                    .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()));
        });
        final Zonky z = harmlessZonky(100_000);
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final PowerTenant auth = mockTenant(z, false);
        final ParticipationDescriptor pd = new ParticipationDescriptor(p, () -> l);
        final Collection<Investment> i = PurchasingSession.purchase(auth, Collections.singleton(pd), s);
        assertThat(i).hasSize(1);
        assertThat(getEventsRequested()).hasSize(5);
        verify(z).purchase(eq(p));
        final RemotePortfolio rp = auth.getPortfolio();
        verify(rp).simulateCharge(eq(l.getId()), eq(l.getRating()), any());
    }
}
