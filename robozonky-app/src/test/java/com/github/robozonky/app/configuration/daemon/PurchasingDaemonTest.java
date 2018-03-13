/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.configuration.daemon;

import java.time.Duration;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class PurchasingDaemonTest extends AbstractZonkyLeveragingTest {

    @Test
    void standard() {
        final Zonky z = harmlessZonky(10_000);
        final Authenticated a = mockAuthentication(z);
        final Portfolio portfolio = Portfolio.create(z, mockBalance(z));
        final Supplier<Optional<PurchaseStrategy>> s = Optional::empty;
        final PurchasingDaemon d = new PurchasingDaemon(t -> {
        }, a, s, () -> Optional.of(portfolio), Duration.ZERO, true);
        d.run();
        verify(z, times(1)).getAvailableParticipations(any());
    }
}
