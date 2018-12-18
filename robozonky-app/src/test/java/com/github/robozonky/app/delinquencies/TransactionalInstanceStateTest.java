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

package com.github.robozonky.app.delinquencies;

import java.util.stream.Stream;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TransactionalInstanceStateTest extends AbstractZonkyLeveragingTest {

    protected static Transactional createTransactional() {
        final Zonky zonky = harmlessZonky(10_000);
        return createTransactional(zonky);
    }

    protected static Transactional createTransactional(final Zonky zonky) {
        final Tenant tenant = mockTenant(zonky);
        return new Transactional(tenant);
    }

    @SuppressWarnings("unchecked")
    @Test
    void delegatesKeys() {
        final InstanceState<String> parent = mock(InstanceState.class);
        when(parent.getKeys()).thenReturn(Stream.empty());
        final TransactionalInstanceState<String> s = new TransactionalInstanceState<>(createTransactional(), parent);
        assertThat(s.getKeys()).isSameAs(parent.getKeys());
    }

}
