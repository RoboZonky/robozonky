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
import com.github.robozonky.api.notifications.RoboZonkyDaemonSuspendedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.remote.entities.Restrictions;
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
import com.github.robozonky.internal.state.InstanceState;
import com.github.robozonky.internal.state.TenantState;
import com.github.robozonky.internal.tenant.Availability;
import com.github.robozonky.internal.tenant.RemotePortfolio;
import com.github.robozonky.internal.tenant.TransactionalTenant;
import org.junit.jupiter.api.Test;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyDaemonSuspended;
import static com.github.robozonky.app.events.impl.EventFactory.sellingCompleted;
import static com.github.robozonky.app.events.impl.EventFactory.sellingCompletedLazy;
import static com.github.robozonky.app.tenant.PowerTenant.transactional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionalPowerTenantImplTest extends AbstractZonkyLeveragingTest {

    private static final SecretProvider SECRETS = SecretProvider.inMemory(SESSION.getUsername());

    private final Zonky zonky = harmlessZonky();
    private final PowerTenant tenant = mockTenant(zonky);
    private final TransactionalPowerTenant transactional = transactional(tenant);

    @Test
    void delegatesAvailability() {
        final Availability result = tenant.getAvailability();
        assertThat(transactional.getAvailability()).isSameAs(result);
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
    void keepsBalance() {
        assertThat(transactional.getKnownBalanceUpperBound()).isEqualTo(Long.MAX_VALUE);
        transactional.setKnownBalanceUpperBound(100);
        assertThat(transactional.getKnownBalanceUpperBound()).isEqualTo(100);
    }

    @Test
    void fires() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        try (final TransactionalPowerTenant t = transactional(new TenantBuilder()
                                                                      .withApi(api)
                                                                      .withSecrets(SECRETS)
                                                                      .build())) {
            final Runnable f = t.fire(roboZonkyDaemonSuspended(new IllegalStateException()));
            t.commit();
            f.run();
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
        final PowerTenant tenant = new TenantBuilder().withApi(api).withSecrets(SECRETS).build();
        try (final TransactionalPowerTenant t = transactional(tenant)) {
            final Runnable f = t.fire(sellingCompletedLazy(() -> sellingCompleted(Collections.emptyList(),
                                                                                  mockPortfolioOverview())));
            t.commit();
            f.run();
        }
        assertThat(this.getEventsRequested())
                .hasSize(1)
                .first().isInstanceOf(SellingCompletedEvent.class);
    }

    @Test
    void failsWhenEventUncommitted() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        final PowerTenant tenant = new TenantBuilder().withApi(api).withSecrets(SECRETS).build();
        try (final TransactionalPowerTenant t = transactional(tenant)) {
            try {
                t.fire(roboZonkyDaemonSuspended(new IllegalStateException()));
                assertThatThrownBy(t::close).isInstanceOf(IllegalStateException.class);
            } finally {
                t.commit(); // clean up after itself, else some locks may be left hanging
            }
        }
    }

    @Test
    void doesNotFailOnNoop() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky();
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

