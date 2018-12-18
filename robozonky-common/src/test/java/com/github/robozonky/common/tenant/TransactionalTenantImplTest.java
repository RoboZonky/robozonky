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

package com.github.robozonky.common.tenant;

import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class TransactionalTenantImplTest {

    private final Tenant original = mockTenant();
    private final TransactionalTenant transactional = new TransactionalTenantImpl(original);

    private static Tenant mockTenant() {
        final Tenant t = mock(Tenant.class);
        when(t.getSessionInfo()).thenReturn(new SessionInfo("somewhere@someone.cz"));
        return t;
    }

    @Test
    void delegatesRestrictions() {
        assertThat(transactional.getRestrictions()).isSameAs(original.getRestrictions());
    }

    @Test
    void delegatesPortfolio() {
        assertThat(transactional.getPortfolio()).isSameAs(original.getPortfolio());
    }

    @Test
    void delegatesSessionInfo() {
        transactional.getSessionInfo();
        verify(original).getSessionInfo();
    }

    @Test
    void delegateAvailability() {
        assertThat(transactional.isAvailable(ZonkyScope.APP)).isSameAs(original.isAvailable(ZonkyScope.APP));
    }

    @Test
    void delegateCall() {
        final Function<Zonky, Zonky> a = Function.identity();
        transactional.call(a, ZonkyScope.FILES);
        verify(original).call(eq(a), eq(ZonkyScope.FILES));
    }

    @Test
    void transactionalState() {
        final InstanceState<String> state = transactional.getState(String.class);
        assertThat(state).isInstanceOf(TransactionalInstanceState.class);
    }

    @Test
    void delegateStrategies() {
        assertThat(transactional.getInvestmentStrategy()).isEmpty();
        verify(original).getInvestmentStrategy();
        assertThat(transactional.getPurchaseStrategy()).isEmpty();
        verify(original).getPurchaseStrategy();
        assertThat(transactional.getSellStrategy()).isEmpty();
        verify(original).getSellStrategy();
    }

    @Test
    void delegateClose() throws Exception {
        transactional.close();
        verify(original).close();
    }

    @Test
    void state() {
        final String key = "a";
        final InstanceState<TransactionalTenantImplTest> s = TenantState.of(transactional.getSessionInfo())
                .in(TransactionalTenantImplTest.class);
        final Tenant orig = mockTenant();
        when(orig.getState(any())).thenAnswer(i -> s);
        final TransactionalTenant copy = new TransactionalTenantImpl(orig);
        final InstanceState<TransactionalTenantImplTest> is = copy.getState(TransactionalTenantImplTest.class);
        is.update(m -> m.put(key, "b"));
        assertThat(s.getKeys()).isEmpty(); // nothing was stored
        copy.commit();
        assertThat(s.getKeys()).containsOnly(key); // now it was persisted
        is.reset();
        copy.commit();
        assertThat(s.getKeys()).isEmpty(); // now it was persisted
    }
}
