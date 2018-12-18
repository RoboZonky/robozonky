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

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.common.tenant.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalTest extends AbstractZonkyLeveragingTest {

    @Test
    void state() {
        final String key = "a";
        final Zonky z = harmlessZonky(10_000);
        final Tenant t = mockTenant(z);
        final Transactional tr = new Transactional(t);
        final InstanceState<TransactionalTest> is = tr.getTenant().getState(TransactionalTest.class);
        is.update(m -> m.put(key, "b"));
        final InstanceState<TransactionalTest> s = TenantState.of(t.getSessionInfo()).in(TransactionalTest.class);
        assertThat(s.getKeys()).isEmpty(); // nothing was stored
        tr.run();
        assertThat(s.getKeys()).containsOnly(key); // now it was persisted
        is.reset();
        tr.run();
        assertThat(s.getKeys()).isEmpty(); // now it was persisted
    }

}
