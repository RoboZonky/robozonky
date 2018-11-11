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

package com.github.robozonky.app.daemon;

import java.util.function.Function;

import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.ZonkyScope;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class TransactionalTenantTest {

    private final Tenant original = mock(Tenant.class);
    private final Tenant transactional = new TransactionalTenant(mock(TransactionalPortfolio.class), original);

    @Test
    void delegatesRestrictions() {
        transactional.getRestrictions();
        verify(original).getRestrictions();
    }

    @Test
    void delegatesSessionInfo() {
        transactional.getSessionInfo();
        verify(original).getSessionInfo();
    }

    @Test
    void delegateSecrets() {
        transactional.getSecrets();
        verify(original).getSecrets();
    }

    @Test
    void delegateAvailability() {
        transactional.isAvailable(ZonkyScope.APP);
        verify(original).isAvailable(eq(ZonkyScope.APP));
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

}
