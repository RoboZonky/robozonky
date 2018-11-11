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

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class PurchasingDaemonTest extends AbstractZonkyLeveragingTest {

    @Test
    void standard() {
        final Zonky z = harmlessZonky(10_000);
        final Tenant a = mockTenant(z);
        final Portfolio portfolio = Portfolio.create(a, BlockedAmountProcessor.createLazy(a));
        final Supplier<Optional<PurchaseStrategy>> s = Optional::empty;
        final PurchasingDaemon d = new PurchasingDaemon(t -> {
        }, a, s, () -> Optional.of(portfolio), Duration.ZERO);
        d.run();
        verify(z, times(1)).getAvailableParticipations(any());
    }

    @Test
    void noBalance() {
        final Zonky z = harmlessZonky(0);
        final Tenant a = mockTenant(z);
        final Portfolio portfolio = Portfolio.create(a, BlockedAmountProcessor.createLazy(a));
        final Supplier<Optional<PurchaseStrategy>> s = Optional::empty;
        final PurchasingDaemon d = new PurchasingDaemon(t -> {
        }, a, s, () -> Optional.of(portfolio), Duration.ZERO);
        d.run();
        verify(z, never()).getAvailableParticipations(any());
    }
}
