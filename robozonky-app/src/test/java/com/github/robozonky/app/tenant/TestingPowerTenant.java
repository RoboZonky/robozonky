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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.LazyEvent;
import com.github.robozonky.test.TestingTenant;

import java.util.concurrent.CompletableFuture;

public class TestingPowerTenant extends TestingTenant implements PowerTenant {

    private final StatefulBoundedBalance balance = new StatefulBoundedBalance(this);

    public TestingPowerTenant(final boolean isDryRun, final Zonky zonky) {
        super(zonky, isDryRun);
    }

    @Override
    public Money getKnownBalanceUpperBound() {
        return balance.get();
    }

    @Override
    public void setKnownBalanceUpperBound(final Money knownBalanceUpperBound) {
        balance.set(knownBalanceUpperBound);
    }

    @Override
    public CompletableFuture<?> fire(final SessionEvent event) {
        return Events.forSession(this).fire(event);
    }

    @Override
    public CompletableFuture<?> fire(final LazyEvent<? extends SessionEvent> event) {
        return Events.forSession(this).fire(event);
    }
}
