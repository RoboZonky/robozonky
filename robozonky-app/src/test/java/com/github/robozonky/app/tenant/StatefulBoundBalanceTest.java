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

import com.github.robozonky.api.Money;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.tenant.Tenant;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class StatefulBoundBalanceTest extends AbstractRoboZonkyTest {

    private final Tenant tenant = mockTenant();

    @Test
    void progression() {
        final StatefulBoundedBalance balance = new StatefulBoundedBalance(tenant);
        assertThat(balance.get()).isEqualTo(Money.from(Long.MAX_VALUE));
        Instant newNow = Instant.now().plus(Duration.ofDays(1));
        setClock(Clock.fixed(newNow, Defaults.ZONE_ID));
        assertThat(balance.get()).isEqualTo(Money.from(Long.MAX_VALUE)); // doesn't change as time moves on
        balance.set(Money.from(1000));
        assertThat(balance.get()).isEqualTo(Money.from(1_000));
        // increases as time moves on
        newNow = newNow.plus(Duration.ofHours(2));
        setClock(Clock.fixed(newNow, Defaults.ZONE_ID));
        assertThat(balance.get())
                .isEqualTo(Money.from(4_096_000))
                .isLessThan(Money.from(Long.MAX_VALUE));
        // resets as too much time passes
        newNow = newNow.plus(Duration.ofDays(1));
        setClock(Clock.fixed(newNow, Defaults.ZONE_ID));
        assertThat(balance.get()).isEqualTo(Money.from(Long.MAX_VALUE));
    }

    @Test
    void persistent() {
        final StatefulBoundedBalance balance = new StatefulBoundedBalance(tenant);
        balance.set(Money.from(1_000));
        final StatefulBoundedBalance balance2 = new StatefulBoundedBalance(tenant);
        assertThat(balance2.get()).isEqualTo(Money.from(1_000)); // old state is read in the new instance, simulating robot restart
    }

    @Test
    void preventsEndlessLoop() {
        final StatefulBoundedBalance balance = new StatefulBoundedBalance(tenant);
        balance.set(Money.from(199));
        setClock(Clock.fixed(Instant.now().plus(Duration.ofDays(1)), Defaults.ZONE_ID));
        assertThat(balance.get()).isEqualTo(Money.from(Long.MAX_VALUE)); // balance is too old, so it is reset to maximum
        balance.set(Money.from(199)); // set it to a different value
        assertThat(balance.get()).isEqualTo(Money.from(199)); // make sure the different value is stored and returned
    }

}
