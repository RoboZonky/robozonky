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
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;
import javax.ws.rs.NotFoundException;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.api.remote.entities.CurrentOverview;
import com.github.robozonky.api.remote.entities.RiskPortfolio;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.Maps;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class UtilTest extends AbstractZonkyLeveragingTest {

    @Test
    void atRisk() {
        final Investment i = Investment.custom()
                .setRating(Rating.D)
                .setRemainingPrincipal(BigDecimal.TEN)
                .setPaidInterest(BigDecimal.ZERO)
                .setPaidPenalty(BigDecimal.ZERO)
                .build();
        final Statistics stats = mock(Statistics.class);
        final RiskPortfolio r = new RiskPortfolio(i.getRating(), 0, 0, i.getRemainingPrincipal().longValue());
        when(stats.getRiskPortfolio()).thenReturn(Collections.singletonList(r));
        when(stats.getCurrentOverview()).thenReturn(mock(CurrentOverview.class));
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getStatistics()).thenReturn(stats);
        when(zonky.getDelinquentInvestments()).thenReturn(Stream.of(i));
        final Tenant tenant = mockTenant(zonky);
        final Map<Rating, BigDecimal> result = Util.getAmountsAtRisk(tenant);
        assertThat(result).containsOnlyKeys(Rating.D);
        assertThat(result.get(Rating.D).longValue()).isEqualTo(BigDecimal.TEN.longValue());
    }

    @Test
    void ignoresBlockedAmountsFromZonky() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final BlockedAmount fee = new BlockedAmount(BigDecimal.ONE);
        final Loan l = Loan.custom().setRating(Rating.D).build();
        final BlockedAmount forLoan = new BlockedAmount(l.getId(), BigDecimal.TEN);
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.of(fee, forLoan));
        when(zonky.getLoan(eq(l.getId()))).thenReturn(l);
        final Map<Integer, Blocked> result = Util.readBlockedAmounts(tenant, zonky.getStatistics());
        assertThat(result).containsOnlyKeys(l.getId());
        assertThat(result.get(l.getId())).isEqualTo(new Blocked(forLoan, Rating.D));
    }

    @Test
    void handles404thrownByZonky() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final BlockedAmount fee = new BlockedAmount(BigDecimal.ONE);
        final BlockedAmount forLoan = new BlockedAmount(1, BigDecimal.TEN);
        when(zonky.getBlockedAmounts()).thenAnswer(i -> Stream.of(fee, forLoan));
        doThrow(new NotFoundException()).when(zonky).getLoan(anyInt());
        assertThatThrownBy(() -> Util.readBlockedAmounts(tenant, zonky.getStatistics()))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(NotFoundException.class);
    }

    @Test
    void zonky404handlingGoesOver() {
        final Divisor d = new Divisor(2000); // to match 5 per mille
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final BlockedAmount forLoan = new BlockedAmount(1, BigDecimal.TEN);
        doThrow(new NotFoundException()).when(zonky).getLoan(anyInt());
        assertThatThrownBy(() -> Util.getLoan(tenant, forLoan, d))
                .isInstanceOf(IllegalStateException.class)
                .hasCauseInstanceOf(NotFoundException.class);
    }

    @Test
    void zonky404handlingGoesUnder() {
        final Divisor d = new Divisor(2001); // to go slightly under 5 per mille
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final BlockedAmount forLoan = new BlockedAmount(1, BigDecimal.TEN);
        doThrow(new NotFoundException()).when(zonky).getLoan(anyInt());
        assertThat(Util.getLoan(tenant, forLoan, d)).isEmpty();
    }

    @Test
    void twoBlockedAmountsFromTheSameLoan() { // most likely culprit is the reservation system
        final Loan loan = Loan.custom().setRating(Rating.D).build();
        final BlockedAmount first = new BlockedAmount(loan.getId(), BigDecimal.TEN);
        final BlockedAmount second = new BlockedAmount(loan.getId(), BigDecimal.ONE);
        final Zonky zonky = harmlessZonky(10_000);
        when(zonky.getLoan(eq(loan.getId()))).thenReturn(loan);
        when(zonky.getBlockedAmounts()).thenReturn(Stream.of(first, second));
        final Tenant tenant = mockTenant(zonky);
        final RemoteData data = RemoteData.load(tenant);
        final Blocked sumOfBoth = new Blocked(first.getAmount().add(second.getAmount()), loan.getRating());
        assertThat(data.getBlocked()).containsOnly(Maps.entry(loan.getId(), sumOfBoth));
    }

}
