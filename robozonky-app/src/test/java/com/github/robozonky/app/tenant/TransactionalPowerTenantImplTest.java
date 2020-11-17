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
import static com.github.robozonky.app.tenant.PowerTenant.transactional;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.RoboZonkyDaemonSuspendedEvent;
import com.github.robozonky.api.notifications.SellingCompletedEvent;
import com.github.robozonky.api.remote.entities.Loan;
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
import com.github.robozonky.test.mock.MockLoanBuilder;

class TransactionalPowerTenantImplTest extends AbstractZonkyLeveragingTest {

    private static final SecretProvider SECRETS = SecretProvider.inMemory(USERNAME);

    private final Zonky zonky = harmlessZonky();
    private final PowerTenant tenant = mockTenant(zonky);
    private final TransactionalPowerTenant transactional = transactional(tenant);

    @Test
    void delegatesAvailability() {
        final Availability result = tenant.getAvailability();
        assertThat(transactional.getAvailability()).isSameAs(result);
    }

    @Test
    void delegatesSession() {
        final SessionInfo result = tenant.getSessionInfo();
        assertThat(transactional.getSessionInfo()).isSameAs(result);
    }

    @Test
    void delegatesInvestmentStrategy() {
        when(tenant.getInvestmentStrategy()).thenReturn(Optional.of(mock(InvestmentStrategy.class)));
        final Optional<InvestmentStrategy> result = tenant.getInvestmentStrategy();
        assertThat(transactional.getInvestmentStrategy()).isEqualTo(result);
    }

    @Test
    void delegatesPurchaseStrategy() {
        when(tenant.getPurchaseStrategy()).thenReturn(Optional.of(mock(PurchaseStrategy.class)));
        final Optional<PurchaseStrategy> result = tenant.getPurchaseStrategy();
        assertThat(transactional.getPurchaseStrategy()).isEqualTo(result);
    }

    @Test
    void delegatesSellStrategy() {
        when(tenant.getSellStrategy()).thenReturn(Optional.of(mock(SellStrategy.class)));
        final Optional<SellStrategy> result = tenant.getSellStrategy();
        assertThat(transactional.getSellStrategy()).isEqualTo(result);
    }

    @Test
    void delegatesReservationStrategy() {
        when(tenant.getReservationStrategy()).thenReturn(Optional.of(mock(ReservationStrategy.class)));
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
        final Loan fresh = MockLoanBuilder.fresh();
        final int loanId = fresh.getId();
        when(zonky.getLoan(anyInt())).thenReturn(fresh);
        final Loan result = tenant.getLoan(loanId);
        assertThat(transactional.getLoan(loanId)).isEqualTo(result);
    }

    @Test
    void keepsBalance() {
        assertThat(transactional.getKnownBalanceUpperBound()).isEqualTo(Money.from(Integer.MAX_VALUE));
        transactional.setKnownBalanceUpperBound(Money.from(100));
        assertThat(transactional.getKnownBalanceUpperBound()).isEqualTo(Money.from(100));
    }

    @Test
    void fires() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        try (final TransactionalPowerTenant t = transactional(new TenantBuilder()
            .withApi(api)
            .withSecrets(SECRETS)
            .build(false))) {
            final CompletableFuture<?> f = t.fire(roboZonkyDaemonSuspended(new IllegalStateException()));
            t.commit();
            f.join();
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
        final PowerTenant tenant = new TenantBuilder().withApi(api)
            .withSecrets(SECRETS)
            .build(false);
        try (var transactional = transactional(tenant)) {
            var future = transactional.fire(sellingCompletedLazy(() -> sellingCompleted(mockPortfolioOverview())));
            transactional.commit();
            future.join();
        }
        assertThat(this.getEventsRequested())
            .hasSize(1)
            .first()
            .isInstanceOf(SellingCompletedEvent.class);
    }

    @Test
    void failsWhenEventUncommitted() throws Exception {
        final OAuth a = mock(OAuth.class);
        final Zonky z = harmlessZonky();
        final ApiProvider api = mockApiProvider(a, z);
        final PowerTenant tenant = new TenantBuilder().withApi(api)
            .withSecrets(SECRETS)
            .build(false);
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
        try (var tenant = transactional(new TenantBuilder()
            .withApi(api)
            .withSecrets(SECRETS)
            .build(false))) {
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
        doAnswer(i -> s).when(orig)
            .getState(any());
        final TransactionalTenant copy = new TransactionalPowerTenantImpl(orig);
        final InstanceState<TransactionalPowerTenantImplTest> is = copy.getState(
                TransactionalPowerTenantImplTest.class);
        is.update(m -> m.put(key, "b"));
        assertThat(s.getKeys()).isEmpty(); // nothing was stored
        copy.commit();
        assertThat(s.getKeys()).containsOnly(key); // now it was persisted
    }

    @Test
    void abort() {
        assertThatThrownBy(() -> tenant.inTransaction(t -> {
            final String key = "a";
            final InstanceState<TransactionalPowerTenantImplTest> s = t
                .getState(TransactionalPowerTenantImplTest.class);
            t.fire(mock(RoboZonkyDaemonSuspendedEvent.class));
            s.update(m -> m.put(key, UUID.randomUUID()
                .toString()));
            throw new IllegalStateException("Should abort transaction.");
        })).isInstanceOf(IllegalStateException.class);
        // nothing is performed
        assertThat(tenant.getState(TransactionalPowerTenantImplTest.class)
            .getKeys()).isEmpty();
        assertThat(getEventsRequested()).isEmpty();
    }
}
