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

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.remote.entities.Restrictions;
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
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.common.tenant.RemotePortfolio;
import com.github.robozonky.common.tenant.TransactionalTenant;
import org.junit.jupiter.api.Test;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonFailed;
import static com.github.robozonky.app.events.impl.EventFactory.sellingCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.sellingCompletedLazy;
import static com.github.robozonky.app.tenant.PowerTenant.transactional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionalPowerTenantImplTest extends AbstractZonkyLeveragingTest {

    private static final SecretProvider SECRETS = SecretProvider.inMemory(SESSION.getUsername());

    private final Zonky zonky = harmlessZonky(10_000);
    private final PowerTenant tenant = mockTenant(zonky);
    private final TransactionalPowerTenant transactional = transactional(tenant);

    @Test
    void delegatesAvailability() {
        final boolean available = tenant.isAvailable(OAuthScope.SCOPE_APP_WEB);
        assertThat(transactional.isAvailable(OAuthScope.SCOPE_APP_WEB)).isEqualTo(available);
    }

    @Test
    void delegatesRestrictions() {
        final Restrictions result = tenant.getRestrictions();
        assertThat(transactional.getRestrictions()).isSameAs(result);
    }

    @Test
    void delegatesSession() {
        final SessionInfo result = tenant.getSessionInfo();
        assertThat(transactional.getSessionInfo()).isSameAs(result);
    }

    @Test
    void delegatesInvestmentStrategy() {
        final Optional<InvestmentStrategy> result = tenant.getInvestmentStrategy();
        assertThat(transactional.getInvestmentStrategy()).isEqualTo(result);
    }

    @Test
    void delegatesPurchaseStrategy() {
        final Optional<PurchaseStrategy> result = tenant.getPurchaseStrategy();
        assertThat(transactional.getPurchaseStrategy()).isEqualTo(result);
    }

    @Test
    void delegatesSellStrategy() {
        final Optional<SellStrategy> result = tenant.getSellStrategy();
        assertThat(transactional.getSellStrategy()).isEqualTo(result);
    }

    @Test
    void delegatesReservationStrategy() {
        final Optional<ReservationStrategy> result = tenant.getReservationStrategy();
        assertThat(transactional.getReservationStrategy()).isEqualTo(result);
    }

    @Test
    void delegatesPortfolio() {
        final RemotePortfolio result = tenant.getPortfolio();
        assertThat(transactional.getPortfolio()).isEqualTo(result);
    }

    @Test
    void delegatesLoan() {
        when(zonky.getLoan(anyInt())).thenReturn(Loan.custom().build());
        final Loan result = tenant.getLoan(1);
        assertThat(transactional.getLoan(1)).isEqualTo(result);
    }

    @Test
    void fires() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky(10_000);
        final ApiProvider api = mockApiProvider(a, z);
        final TransactionalPowerTenant t = transactional(new TenantBuilder()
                                                                 .withApi(api)
                                                                 .withSecrets(SECRETS)
                                                                 .build());
        try {
            final Runnable f = t.fire(roboZonkyDaemonFailed(new IllegalStateException()));
            t.commit();
            f.run();
        } finally {
            t.close();
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
        final TransactionalPowerTenant t = transactional(new TenantBuilder()
                                                                 .withApi(api)
                                                                 .withSecrets(SECRETS)
                                                                 .build());
        try {
            final Runnable f = t.fire(sellingCompletedLazy(() -> sellingCompleted(Collections.emptyList(),
                                                                                  mockPortfolioOverview(10_000))));
            t.commit();
            f.run();
        } finally {
            t.close();
        }
        assertThat(this.getEventsRequested())
                .hasSize(1)
                .first().isInstanceOf(SellingCompletedEvent.class);
    }

    @Test
    void failsWhenEventUncommitted() {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky(10_000);
        final ApiProvider api = mockApiProvider(a, z);
        final TransactionalPowerTenant t = transactional(new TenantBuilder()
                                                                 .withApi(api)
                                                                 .withSecrets(SECRETS)
                                                                 .build());
        try {
            t.fire(roboZonkyDaemonFailed(new IllegalStateException()));
        } finally {
            assertThatThrownBy(t::close).isInstanceOf(IllegalStateException.class);
        }
    }

    @Test
    void doesNotFailOnNoop() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky(10_000);
        final ApiProvider api = mockApiProvider(a, z);
        try (final TransactionalPowerTenant t = transactional(new TenantBuilder()
                                                                      .withApi(api)
                                                                      .withSecrets(SECRETS)
                                                                      .build())) {
            // do nothing with the tenant
        }
    }

    @Test
    void transactionalState() {
        final InstanceState<String> state = transactional.getState(String.class);
        assertThat(state).isInstanceOf(TransactionalInstanceState.class);
    }

    @Test
    void state() {
        final String key = "a";
        final InstanceState<TransactionalPowerTenantImplTest> s = TenantState.of(transactional.getSessionInfo())
                .in(TransactionalPowerTenantImplTest.class);
        final PowerTenant orig = mockTenant();
        doAnswer(i -> s).when(orig).getState(any());
        final TransactionalTenant copy = new TransactionalPowerTenantImpl(orig);
        final InstanceState<TransactionalPowerTenantImplTest> is = copy.getState(
                TransactionalPowerTenantImplTest.class);
        is.update(m -> m.put(key, "b"));
        assertThat(s.getKeys()).isEmpty(); // nothing was stored
        copy.commit();
        assertThat(s.getKeys()).containsOnly(key); // now it was persisted
    }
}

