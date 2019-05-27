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

package com.github.robozonky.app.tenant;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.remote.Zonky;
import com.github.robozonky.internal.api.tenant.RemotePortfolio;
import com.github.robozonky.internal.api.tenant.Tenant;
import com.github.robozonky.internal.test.DateUtil;
import io.vavr.Tuple;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RemotePortfolioImplTest extends AbstractZonkyLeveragingTest {

    static Stream<Arguments> parameters() {
        return Stream.of(
            Arguments.arguments(0, 0, 2),
            Arguments.arguments(0, 1, 2),
            Arguments.arguments(0, 59, 2),
            Arguments.arguments(1, 0, 2),
            Arguments.arguments(1, 1, 2),
            Arguments.arguments(1, 59, 2),
            Arguments.arguments(2, 0, 2),
            Arguments.arguments(2, 1, 2),
            Arguments.arguments(2, 59, 2),
            Arguments.arguments(7, 0, 7),
            Arguments.arguments(7, 1, 7),
            Arguments.arguments(7, 59, 7),
            Arguments.arguments(8, 0, 11),
            Arguments.arguments(8, 1, 11),
            Arguments.arguments(8, 59, 11),
            Arguments.arguments(9, 0, 11),
            Arguments.arguments(9, 1, 11),
            Arguments.arguments(9, 59, 11),
            Arguments.arguments(10, 0, 11),
            Arguments.arguments(10, 1, 11),
            Arguments.arguments(10, 59, 11),
            Arguments.arguments(11, 0, 11),
            Arguments.arguments(11, 1, 11),
            Arguments.arguments(11, 59, 11),
            Arguments.arguments(22, 0, 22),
            Arguments.arguments(22, 1, 22),
            Arguments.arguments(22, 59, 22),
            Arguments.arguments(23, 0, 2),
            Arguments.arguments(23, 1, 2),
            Arguments.arguments(23, 59, 2)
        );
    }

    @ParameterizedTest
    @MethodSource("parameters")
    void sellabilityRefresh(final int serverHourWhenChecking, final int serverMinuteWhenChecking,
                            final int expectedHour) {
        final ZoneId zone = ZoneId.of("Europe/Prague");
        final ZonedDateTime checkTime = LocalTime.of(serverHourWhenChecking, serverMinuteWhenChecking, 0)
                .atDate(LocalDate.now())
                .atZone(zone);
        final ZonedDateTime calculationTime = checkTime.minus(RemotePortfolioImpl.SELLABLE_REFRESH);
        DateUtil.setSystemClock(Clock.fixed(calculationTime.toInstant(), zone));
        final Duration result =
                RemotePortfolioImpl.getSellableRefresh(Tuple.of(Collections.emptyMap(), Collections.emptyMap()));
        final ZonedDateTime actual = calculationTime.plus(result);
        assertThat(actual.getHour())
                .as("Refresh not triggered when expected, was: " + actual)
                .isEqualTo(expectedHour);
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
            softly.assertThat(p.getTotal()).containsOnly(Map.entry(Rating.D, firstAmount));
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount);
        });
        // new blocked amount for a different loan is registered locally
        final BigDecimal secondAmount = BigDecimal.TEN;
        final Loan l2 = Loan.custom().setRating(Rating.A).build();
        p.simulateCharge(l2.getId(), l2.getRating(), secondAmount);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(
                    Map.entry(Rating.D, firstAmount),
                    Map.entry(Rating.A, secondAmount)
            );
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount.add(secondAmount));
        });
        /*
         * time passed, the same blocked amount is read from Zonky and the old one needs to be replaced.
         * portfolio totals must not change, as there was no change - just the synthetic is replaced by real amount
         */
        setClock(Clock.fixed(Instant.now().plus(Duration.ofMinutes(5)), Defaults.ZONE_ID));
        when(zonky.getStatistics()).thenReturn(Statistics.emptyAndFresh());
        when(zonky.getBlockedAmounts())
                .thenAnswer(i -> Stream.of(original, new BlockedAmount(l2.getId(), secondAmount)));
        when(zonky.getLoan(eq(l2.getId()))).thenReturn(l2);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(
                    Map.entry(Rating.D, firstAmount),
                    Map.entry(Rating.A, secondAmount)
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
        final Instant now = Instant.now();
        setClock(Clock.fixed(now, Defaults.ZONE_ID));
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(Map.entry(Rating.D, firstAmount));
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount);
        });
        // new blocked amount for a different loan is registered locally
        final BigDecimal secondAmount = BigDecimal.TEN;
        final Loan l2 = Loan.custom().setRating(Rating.A).build();
        p.simulateCharge(l2.getId(), l2.getRating(), secondAmount);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(
                    Map.entry(Rating.D, firstAmount),
                    Map.entry(Rating.A, secondAmount)
            );
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount.add(secondAmount));
        });
        /*
         * time passed, the same blocked amount is read from Zonky and must be added to the synthetic one from the
         * dry run.
         */
        setClock(Clock.fixed(now.plus(Duration.ofMinutes(6)), Defaults.ZONE_ID));
        when(zonky.getBlockedAmounts())
                .thenAnswer(i -> Stream.of(original, new BlockedAmount(l2.getId(), secondAmount)));
        when(zonky.getLoan(eq(l2.getId()))).thenReturn(l2);
        final BigDecimal total = secondAmount.add(secondAmount);
        assertSoftly(softly -> {
            softly.assertThat(p.getTotal()).containsOnly(
                    Map.entry(Rating.D, firstAmount),
                    Map.entry(Rating.A, total)
            );
            softly.assertThat(p.getOverview().getCzkInvested()).isEqualTo(firstAmount.add(total));
        });
    }

}
