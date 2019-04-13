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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.RemotePortfolio;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.Maps;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RemotePortfolioImplTest extends AbstractZonkyLeveragingTest {

    @Test
    void cachesRemoteData() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        p.getOverview(); // read everything once
        verify(zonky).getWallet();
        verify(zonky).getStatistics();
        verify(zonky).getDelinquentInvestments();
        verify(zonky).getBlockedAmounts();
        p.getOverview(); // it is cached, does not re-read
        verify(zonky, times(1)).getWallet();
        verify(zonky, times(1)).getStatistics();
        verify(zonky, times(1)).getDelinquentInvestments();
        verify(zonky, times(1)).getBlockedAmounts();
        final Instant fiveMinutesLater = Instant.now()
                .plus(Duration.ofMinutes(5))
                .plus(Duration.ofSeconds(1));
        setClock(Clock.fixed(fiveMinutesLater, Defaults.ZONE_ID)); // move clock past refresh
        p.getOverview();
        verify(zonky, times(2)).getWallet();
        verify(zonky, times(2)).getStatistics();
        verify(zonky, times(1)).getDelinquentInvestments(); // this is on a longer cycle
        verify(zonky, times(2)).getBlockedAmounts();
        final Instant thirtyFiveMinutesLater = Instant.now()
                .plus(Duration.ofMinutes(35));
        setClock(Clock.fixed(thirtyFiveMinutesLater, Defaults.ZONE_ID)); // move clock past refresh
        p.getOverview();
        verify(zonky, times(3)).getWallet();
        verify(zonky, times(3)).getStatistics();
        verify(zonky, times(2)).getDelinquentInvestments();
        verify(zonky, times(3)).getBlockedAmounts();
    }

    @Test
    void throwsWhenRemoteFails() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        doThrow(IllegalStateException.class).when(zonky).getStatistics();
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        assertThatThrownBy(p::getOverview)
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(IllegalStateException.class);
    }

    @Test
    void balanceReflectsCharges() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        assertThat(p.getBalance().intValue()).isEqualTo(10_000);
        p.simulateCharge(1, Rating.D, BigDecimal.TEN);
        assertThat(p.getBalance().intValue()).isEqualTo(9_990);
    }

    @Test
    void amountsAtRisk() {
        final Loan l1 = Loan.custom().setRating(Rating.A).setAmount(10_000).build();
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(
                Investment.fresh(l1, BigDecimal.ONE).build()
        ));
        final Tenant tenant = mockTenant(zonky);
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        assertThat(p.getAtRisk()).containsOnlyKeys(Rating.A)
                .containsValues(BigDecimal.ONE);
    }

    @Test
    void chargesAffectAmounts() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        assertThat(p.getTotal()).isEmpty();
        assertThat(p.getOverview().getCzkInvested()).isEqualTo(BigDecimal.ZERO);
        assertThat(p.getOverview().getCzkAtRisk()).isEqualTo(BigDecimal.ZERO);
        assertThat(p.getOverview().getCzkAvailable()).isEqualTo(BigDecimal.valueOf(10_000));
        p.simulateCharge(1, Rating.D, BigDecimal.TEN);
        assertThat(p.getTotal()).containsOnlyKeys(Rating.D)
                .containsValues(BigDecimal.TEN);
        p.simulateCharge(2, Rating.A, BigDecimal.ONE);
        assertThat(p.getTotal()).containsOnlyKeys(Rating.A, Rating.D)
                .containsValues(BigDecimal.ONE, BigDecimal.TEN);
        p.simulateCharge(3, Rating.A, BigDecimal.ONE);
        assertThat(p.getTotal()).containsOnlyKeys(Rating.A, Rating.D)
                .containsValues(BigDecimal.valueOf(2), BigDecimal.TEN);
    }

    @Test
    void chargesAreProperlyReplacedByRemotesOutsideDryRun() {
        final BigDecimal firstAmount = BigDecimal.ONE;
        final Loan l = Loan.custom().setRating(Rating.D).build();
        final BlockedAmount original = new BlockedAmount(l.getId(), firstAmount);
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(l.getId()))).thenReturn(l);
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.of(original));
        final Tenant tenant = mockTenant(zonky, false);
        // one blocked amount coming in remotely
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(Maps.entry(Rating.D, firstAmount));
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount);
        });
        // new blocked amount for a different loan is registered locally
        final BigDecimal secondAmount = BigDecimal.TEN;
        final Loan l2 = Loan.custom().setRating(Rating.A).build();
        p.simulateCharge(l2.getId(), l2.getRating(), secondAmount);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(
                    Maps.entry(Rating.D, firstAmount),
                    Maps.entry(Rating.A, secondAmount)
            );
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount.add(secondAmount));
        });
        /*
         * time passed, the same blocked amount is read from Zonky and the old one needs to be replaced.
         * portfolio totals must not change, as there was no change - just the synthetic is replaced by real amount
         */
        setClock(Clock.fixed(Instant.now().plus(Duration.ofMinutes(5)), Defaults.ZONE_ID));
        when(zonky.getBlockedAmounts())
                .thenAnswer(i -> Stream.of(original, new BlockedAmount(l2.getId(), secondAmount)));
        when(zonky.getLoan(eq(l2.getId()))).thenReturn(l2);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(
                    Maps.entry(Rating.D, firstAmount),
                    Maps.entry(Rating.A, secondAmount)
            );
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount.add(secondAmount));
        });
    }

    @Test
    void chargesAreNeverReplacedByRemotesInDryRun() {
        final BigDecimal firstAmount = BigDecimal.ONE;
        final Loan l = Loan.custom().setRating(Rating.D).build();
        final BlockedAmount original = new BlockedAmount(l.getId(), firstAmount);
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(l.getId()))).thenReturn(l);
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.of(original));
        final Tenant tenant = mockTenant(zonky, true);
        // one blocked amount coming in remotely
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(Maps.entry(Rating.D, firstAmount));
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount);
        });
        // new blocked amount for a different loan is registered locally
        final BigDecimal secondAmount = BigDecimal.TEN;
        final Loan l2 = Loan.custom().setRating(Rating.A).build();
        p.simulateCharge(l2.getId(), l2.getRating(), secondAmount);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(
                    Maps.entry(Rating.D, firstAmount),
                    Maps.entry(Rating.A, secondAmount)
            );
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount.add(secondAmount));
        });
        /*
         * time passed, the same blocked amount is read from Zonky and must be added to the synthetic one from the
         * dry run.
         */
        setClock(Clock.fixed(Instant.now().plus(Duration.ofMinutes(5)), Defaults.ZONE_ID));
        when(zonky.getBlockedAmounts())
                .thenAnswer(i -> Stream.of(original, new BlockedAmount(l2.getId(), secondAmount)));
        when(zonky.getLoan(eq(l2.getId()))).thenReturn(l2);
        final BigDecimal total = secondAmount.add(secondAmount);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(
                    Maps.entry(Rating.D, firstAmount),
                    Maps.entry(Rating.A, total)
            );
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount.add(total));
        });
    }

}
