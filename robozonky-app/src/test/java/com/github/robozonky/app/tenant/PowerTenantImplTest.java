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

import java.util.Collections;
import java.util.Optional;

import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.OAuthScope;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.OAuth;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonFailed;
import static com.github.robozonky.app.events.impl.EventFactory.sellingCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.sellingCompletedLazy;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PowerTenantImplTest extends AbstractZonkyLeveragingTest {

    private static final SecretProvider SECRETS = SecretProvider.inMemory(SESSION.getUsername());

    @Test
    void closesWhenNoTokens() {
        final OAuth a = mock(OAuth.class);
        final Zonky z = mock(Zonky.class);
        final ApiProvider api = mockApiProvider(a, z);
        try (final Tenant tenant = new TenantBuilder().withSecrets(SECRETS).withApi(api).build()) {
            assertThat(tenant.isAvailable()).isTrue();
        } catch (final Exception e) {
            fail(e);
        }
        verifyZeroInteractions(a);
        verifyZeroInteractions(z);
    }

    @Test
    void closesWithTokens() {
        final OAuth a = mock(OAuth.class);
        when(a.login(any(), any(), any())).thenReturn(mock(ZonkyApiToken.class));
        final Zonky z = harmlessZonky(10_000);
        final ApiProvider api = mockApiProvider(a, z);
        try (final Tenant tenant = new TenantBuilder().withSecrets(SECRETS).withApi(api).build()) {
            final Statistics s = tenant.call(Zonky::getStatistics);
            assertThat(s).isSameAs(Statistics.empty());
            assertThat(tenant.isAvailable()).isTrue();
        } catch (final Exception e) {
            fail(e);
        }
        verify(a).login(any(), any(), any());
        verify(z).logout();
    }

    @Test
    void strategies() {
        final StrategyProvider s = mock(StrategyProvider.class);
        when(s.getToInvest()).thenReturn(Optional.of(mock(InvestmentStrategy.class)));
        when(s.getToSell()).thenReturn(Optional.of(mock(SellStrategy.class)));
        when(s.getToPurchase()).thenReturn(Optional.of(mock(PurchaseStrategy.class)));
        when(s.getForReservations()).thenReturn(Optional.of(mock(ReservationStrategy.class)));
        final PowerTenantImpl t = new PowerTenantImpl(SESSION_DRY, null, null, () -> s, null);
        assertThat(t.getInvestmentStrategy()).containsInstanceOf(InvestmentStrategy.class);
        assertThat(t.getSellStrategy()).containsInstanceOf(SellStrategy.class);
        assertThat(t.getPurchaseStrategy()).containsInstanceOf(PurchaseStrategy.class);
        assertThat(t.getReservationStrategy()).containsInstanceOf(ReservationStrategy.class);
    }

    @Test
    void availabilityOfToken() {
        final ZonkyApiTokenSupplier s = mock(ZonkyApiTokenSupplier.class);
        final PowerTenantImpl t = new PowerTenantImpl(SESSION_DRY, null, () -> true, () -> null, scope -> s);
        assertThat(t.isAvailable(OAuthScope.SCOPE_APP_WEB)).isTrue();
        when(s.isClosed()).thenReturn(true);
        assertThat(t.isAvailable(OAuthScope.SCOPE_APP_WEB)).isFalse();
    }

    @Test
    void availabilityOfZonky() {
        final ZonkyApiTokenSupplier s = mock(ZonkyApiTokenSupplier.class);
        final PowerTenantImpl t = new PowerTenantImpl(SESSION_DRY, null, () -> false, () -> null, scope -> s);
        assertThat(t.isAvailable(OAuthScope.SCOPE_APP_WEB)).isFalse();
        when(s.isClosed()).thenReturn(true); // token availability makes no difference
        assertThat(t.isAvailable(OAuthScope.SCOPE_APP_WEB)).isFalse();
    }

    @Test
    void getters() throws Exception {
        final OAuth a = mock(OAuth.class);
        when(a.login(any(), any(), any())).thenReturn(mock(ZonkyApiToken.class));
        final Zonky z = harmlessZonky(10_000);
        final Loan l = Loan.custom().setId(1).build();
        when(z.getLoan(eq(1))).thenReturn(l);
        doThrow(IllegalStateException.class).when(z).getRestrictions(); // will result in full restrictions
        final ApiProvider api = mockApiProvider(a, z);
        try (final Tenant tenant = new TenantBuilder().withApi(api).withSecrets(SECRETS).build()) {
            assertThat(tenant.getLoan(1)).isSameAs(l);
            assertThat(tenant.getPortfolio()).isNotNull();
            assertThat(tenant.getState(PowerTenantImpl.class)).isNotNull();
            final Restrictions r = tenant.getRestrictions();
            assertThat(r.isCannotAccessSmp()).isTrue();
            assertThat(r.isCannotInvest()).isTrue();
        }
    }

    @Test
    void fires() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky(10_000);
        final ApiProvider api = mockApiProvider(a, z);
        try (final PowerTenant tenant = new TenantBuilder().withApi(api).withSecrets(SECRETS).build()) {
            tenant.fire(roboZonkyDaemonFailed(new IllegalStateException()));
        }
        assertThat(this.getEventsRequested())
                .hasSize(1)
                .first().isInstanceOf(RoboZonkyDaemonFailedEvent.class);
    }

    @Test
    void firesLazy() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky(10_000);
        final ApiProvider api = mockApiProvider(a, z);
        try (final PowerTenant tenant = new TenantBuilder().withApi(api).withSecrets(SECRETS).build()) {
            tenant.fire(sellingCompletedLazy(() -> sellingCompleted(Collections.emptyList(),
                                                                    mockPortfolioOverview(10_000))))
                    .run();
        }
        assertThat(this.getEventsRequested())
                .hasSize(1)
                .first().isInstanceOf(SellingCompletedEvent.class);
    }
}
