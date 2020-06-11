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

package com.github.robozonky.app.tenant;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonSuspended;
import static com.github.robozonky.app.events.impl.EventFactory.sellingCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.sellingCompletedLazy;
import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.RoboZonkyDaemonSuspendedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.SellFee;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.entities.SellPriceInfo;
import com.github.robozonky.api.remote.entities.Statistics;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.SellStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.remote.OAuth;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.remote.entities.LoanImpl;
import com.github.robozonky.internal.remote.entities.SellFeeImpl;
import com.github.robozonky.internal.remote.entities.SellInfoImpl;
import com.github.robozonky.internal.remote.entities.SellPriceInfoImpl;
import com.github.robozonky.internal.remote.entities.StatisticsImpl;
import com.github.robozonky.internal.secrets.SecretProvider;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.mock.MockLoanBuilder;

class PowerTenantImplTest extends AbstractZonkyLeveragingTest {

    private static final SessionInfo SESSION = mockSessionInfo();
    private static final SecretProvider SECRETS = mockSecretProvider();
    private static final ZonkyApiToken TOKEN = SECRETS.getToken()
        .get();

    @Test
    void closesWithTokens() {
        final OAuth a = mock(OAuth.class);
        when(a.refresh(eq(TOKEN))).thenReturn(TOKEN);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        final PowerTenant tenant = new TenantBuilder().withSecrets(SECRETS)
            .withApi(api)
            .build(false);
        try (tenant) {
            final Statistics s = tenant.call(Zonky::getStatistics);
            assertThat(s).isSameAs(StatisticsImpl.empty());
        } catch (final Exception e) {
            fail(e);
        }
        verify(a).refresh(any());
        assertThatThrownBy(() -> tenant.getLoan(1)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void strategies() {
        final StrategyProvider s = mock(StrategyProvider.class);
        when(s.getToInvest()).thenReturn(Optional.of(mock(InvestmentStrategy.class)));
        when(s.getToSell()).thenReturn(Optional.of(mock(SellStrategy.class)));
        when(s.getToPurchase()).thenReturn(Optional.of(mock(PurchaseStrategy.class)));
        when(s.getForReservations()).thenReturn(Optional.of(mock(ReservationStrategy.class)));
        final PowerTenantImpl t = new PowerTenantImpl(SESSION.getUsername(), SESSION.getName(), true, new ApiProvider(),
                s, null);
        assertThat(t.getInvestmentStrategy()).containsInstanceOf(InvestmentStrategy.class);
        assertThat(t.getSellStrategy()).containsInstanceOf(SellStrategy.class);
        assertThat(t.getPurchaseStrategy()).containsInstanceOf(PurchaseStrategy.class);
        assertThat(t.getReservationStrategy()).containsInstanceOf(ReservationStrategy.class);
    }

    @Test
    void getters() throws Exception {
        final SecretProvider s = mockSecretProvider();
        final ZonkyApiToken token = s.getToken()
            .get();
        final OAuth a = mock(OAuth.class);
        when(a.refresh(eq(token))).thenReturn(token);
        final Zonky z = harmlessZonky();
        final Loan l = new MockLoanBuilder()
            .set(LoanImpl::setRemainingInvestment, Money.from(1_000))
            .build();
        when(z.getLoan(eq(l.getId()))).thenReturn(l);
        final SellFee sf = mock(SellFeeImpl.class);
        when(sf.getExpiresAt()).thenReturn(Optional.of(OffsetDateTime.ofInstant(Instant.EPOCH, Defaults.ZONE_ID)));
        final SellPriceInfo spi = mock(SellPriceInfoImpl.class);
        when(spi.getFee()).thenReturn(sf);
        final SellInfo si = mock(SellInfoImpl.class);
        when(si.getPriceInfo()).thenReturn(spi);
        when(z.getSellInfo(anyLong())).thenReturn(si);
        doThrow(IllegalStateException.class).when(z)
            .getRestrictions(); // will result in full restrictions
        final ApiProvider api = mockApiProvider(a, z);
        try (final Tenant tenant = new TenantBuilder().withApi(api)
            .withSecrets(s)
            .build(false)) {
            assertThat(tenant.getAvailability()).isNotNull();
            assertThat(tenant.getLoan(l.getId())).isSameAs(l);
            assertThat(tenant.getSellInfo(1)).isSameAs(si);
            assertThat(tenant.getPortfolio()).isNotNull();
            assertThat(tenant.getSessionInfo()).isNotNull();
            assertThat(tenant.getState(PowerTenantImpl.class)).isNotNull();
        }
    }

    @Test
    void keepsBalance() throws Exception {
        try (final PowerTenant tenant = new TenantBuilder().withSecrets(SECRETS)
            .build(false)) {
            assertThat(tenant.getKnownBalanceUpperBound()).isEqualTo(StatefulBoundedBalance.MAXIMUM);
            tenant.setKnownBalanceUpperBound(Money.from(100));
            assertThat(tenant.getKnownBalanceUpperBound()).isEqualTo(Money.from(100));
        }
    }

    @Test
    void fires() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        try (final PowerTenant tenant = new TenantBuilder().withApi(api)
            .withSecrets(SECRETS)
            .build(false)) {
            tenant.fire(roboZonkyDaemonSuspended(new IllegalStateException()))
                .join();
        }
        assertThat(this.getEventsRequested())
            .hasSize(1)
            .first()
            .isInstanceOf(RoboZonkyDaemonSuspendedEvent.class);
    }

    @Test
    void firesLazy() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        try (final PowerTenant tenant = new TenantBuilder().withApi(api)
            .withSecrets(SECRETS)
            .build(false)) {
            tenant.fire(sellingCompletedLazy(() -> sellingCompleted(Collections.emptyList(),
                    mockPortfolioOverview())))
                .join();
        }
        assertThat(this.getEventsRequested())
            .hasSize(1)
            .first()
            .isInstanceOf(SellingCompletedEvent.class);
    }
}
