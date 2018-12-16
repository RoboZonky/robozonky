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

package com.github.robozonky.app.authentication;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.RemotePortfolio;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RemotePortfolioImplTest extends AbstractZonkyLeveragingTest {

    @Test
    void cachesRemoteData() {
        final Zonky zonky = harmlessZonky(10_000);
        final Tenant tenant = mockTenant(zonky);
        final RemotePortfolio p = new RemotePortfolioImpl(tenant);
        p.getBalance(); // read everything one
        verify(zonky).getWallet();
        verify(zonky).getStatistics();
        verify(zonky).getDelinquentInvestments();
        verify(zonky).getBlockedAmounts();
        p.getBalance(); // it is cached, does not re-read
        verify(zonky, times(1)).getWallet();
        verify(zonky, times(1)).getStatistics();
        verify(zonky, times(1)).getDelinquentInvestments();
        verify(zonky, times(1)).getBlockedAmounts();
        final Instant fiveMinutesLater = Instant.now()
                .plus(Duration.ofMinutes(5))
                .plus(Duration.ofSeconds(1));
        setClock(Clock.fixed(fiveMinutesLater, Defaults.ZONE_ID)); // move clock past refresh
        p.getBalance();
        verify(zonky, times(2)).getWallet();
        verify(zonky, times(2)).getStatistics();
        verify(zonky, times(2)).getDelinquentInvestments();
        verify(zonky, times(2)).getBlockedAmounts();
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

}
