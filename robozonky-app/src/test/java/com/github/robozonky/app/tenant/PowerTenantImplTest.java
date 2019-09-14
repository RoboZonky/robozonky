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

import com.github.robozonky.api.notifications.RoboZonkyDaemonSuspendedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.remote.OAuth;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.internal.tenant.Tenant;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;
import java.util.UUID;

import static com.github.robozonky.app.events.impl.EventFactory.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

class PowerTenantImplTest extends AbstractZonkyLeveragingTest {

    private static final SecretProvider SECRETS = SecretProvider.inMemory(SESSION.getUsername());
    private static final ZonkyApiToken TOKEN = new ZonkyApiToken(UUID.randomUUID().toString(),
                                                                 UUID.randomUUID().toString(), 299);

    @Test
    void closesWithTokens() {
        final OAuth a = mock(OAuth.class);
        when(a.login(any())).thenReturn(TOKEN);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        final Tenant tenant = new TenantBuilder().withSecrets(SECRETS).withApi(api).build();
        try (tenant) {
            final Statistics s = tenant.call(Zonky::getStatistics);
            assertThat(s).isSameAs(Statistics.empty());
        } catch (final Exception e) {
            fail(e);
        }
        verify(a).login(any());
        assertThatThrownBy(() -> tenant.getLoan(1)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> tenant.getInvestment(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void strategies() {
        final StrategyProvider s = mock(StrategyProvider.class);
        when(s.getToInvest()).thenReturn(Optional.of(mock(InvestmentStrategy.class)));
        when(s.getToSell()).thenReturn(Optional.of(mock(SellStrategy.class)));
        when(s.getToPurchase()).thenReturn(Optional.of(mock(PurchaseStrategy.class)));
        when(s.getForReservations()).thenReturn(Optional.of(mock(ReservationStrategy.class)));
        final PowerTenantImpl t = new PowerTenantImpl(SESSION_DRY, new ApiProvider(), () -> s, null);
        assertThat(t.getInvestmentStrategy()).containsInstanceOf(InvestmentStrategy.class);
        assertThat(t.getSellStrategy()).containsInstanceOf(SellStrategy.class);
        assertThat(t.getPurchaseStrategy()).containsInstanceOf(PurchaseStrategy.class);
        assertThat(t.getReservationStrategy()).containsInstanceOf(ReservationStrategy.class);
    }

    @Test
    void getters() throws Exception {
        final OAuth a = mock(OAuth.class);
        when(a.login(any())).thenReturn(TOKEN);
        final Zonky z = harmlessZonky();
        final Loan l = Loan.custom().setId(1).build();
        final Investment i = Investment.fresh(l, 200).build();
        when(z.getLoan(eq(1))).thenReturn(l);
        when(z.getInvestmentByLoanId(eq(1))).thenReturn(Optional.of(i));
        doThrow(IllegalStateException.class).when(z).getRestrictions(); // will result in full restrictions
        final ApiProvider api = mockApiProvider(a, z);
        try (final Tenant tenant = new TenantBuilder().withApi(api).withSecrets(SECRETS).build()) {
            assertThat(tenant.getAvailability()).isNotNull();
            assertThat(tenant.getLoan(1)).isSameAs(l);
            assertThat(tenant.getInvestment(1)).isSameAs(i);
            assertThat(tenant.getPortfolio()).isNotNull();
            assertThat(tenant.getState(PowerTenantImpl.class)).isNotNull();
            final Restrictions r = tenant.getRestrictions();
            assertThat(r.isCannotAccessSmp()).isTrue();
            assertThat(r.isCannotInvest()).isTrue();
        }
    }

    @Test
    void keepsBalance() throws Exception {
        try (final PowerTenant tenant = new TenantBuilder().withSecrets(SECRETS).build()) {
            assertThat(tenant.getKnownBalanceUpperBound()).isEqualTo(Long.MAX_VALUE);
            tenant.setKnownBalanceUpperBound(100);
            assertThat(tenant.getKnownBalanceUpperBound()).isEqualTo(100);
        }
    }

    @Test
    void fires() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        try (final PowerTenant tenant = new TenantBuilder().withApi(api).withSecrets(SECRETS).build()) {
            tenant.fire(roboZonkyDaemonSuspended(new IllegalStateException())).run();
        }
        assertThat(this.getEventsRequested())
                .hasSize(1)
                .first().isInstanceOf(RoboZonkyDaemonSuspendedEvent.class);
    }

    @Test
    void firesLazy() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        try (final PowerTenant tenant = new TenantBuilder().withApi(api).withSecrets(SECRETS).build()) {
            tenant.fire(sellingCompletedLazy(() -> sellingCompleted(Collections.emptyList(),
                                                                    mockPortfolioOverview())))
                    .run();
        }
        assertThat(this.getEventsRequested())
                .hasSize(1)
                .first().isInstanceOf(SellingCompletedEvent.class);
    }
}
