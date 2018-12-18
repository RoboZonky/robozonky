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

package com.github.robozonky.app.daemon;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.daemon.transactions.SoldParticipationCache;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PurchasingDaemonTest extends AbstractZonkyLeveragingTest {

    @Test
    void standard() {
        final Zonky z = harmlessZonky(10_000);
        final PowerTenant a = mockTenant(z);
        final PurchasingDaemon d = new PurchasingDaemon(t -> {
        }, a, Duration.ZERO);
        d.run();
        verify(z, times(1)).getAvailableParticipations(any());
    }

    @Test
    void ignoringSoldBefore() {
        final int loanId = 1;
        final Participation p = mock(Participation.class);
        when(p.getLoanId()).thenReturn(loanId);
        final Participation p2 = mock(Participation.class);
        when(p2.getLoanId()).thenReturn(loanId + 1);
        when(p2.getRemainingPrincipal()).thenReturn(BigDecimal.TEN);
        final Zonky z = harmlessZonky(10_000);
        when(z.getAvailableParticipations(any())).thenReturn(Stream.of(p, p2));
        when(z.getLoan(anyInt())).thenAnswer(i -> Loan.custom()
                .setId(i.getArgument(0))
                .setRating(Rating.D)
                .build());
        final PowerTenant a = mockTenant(z);
        when(a.getSessionInfo()).thenReturn(SESSION); // no dry run
        when(a.getPurchaseStrategy()).thenReturn(Optional.of((available, portfolio, restrictions) -> available.stream()
                .map(ParticipationDescriptor::recommend)
                .flatMap(s -> s.map(Stream::of).orElse(Stream.empty())))); // recommend all participations
        SoldParticipationCache.forTenant(a).markAsSold(loanId);
        final PurchasingDaemon d = new PurchasingDaemon(t -> {
        }, a, Duration.ZERO);
        d.run();
        verify(z, never()).purchase(eq(p));
        verify(z).purchase(eq(p2));
    }

    @Test
    void noBalance() {
        final Zonky z = harmlessZonky(0);
        final PowerTenant a = mockTenant(z);
        final PurchasingDaemon d = new PurchasingDaemon(t -> {
        }, a, Duration.ZERO);
        d.run();
        verify(z, never()).getAvailableParticipations(any());
    }
}
