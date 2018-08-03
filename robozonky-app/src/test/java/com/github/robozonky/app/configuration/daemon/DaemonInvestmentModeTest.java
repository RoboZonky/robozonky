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
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZonedDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.internal.api.Defaults;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class DaemonInvestmentModeTest extends AbstractZonkyLeveragingTest {

    private final Lifecycle lifecycle = new Lifecycle();

    @Test
    void get() throws Exception {
        final Tenant a = mockTenant(harmlessZonky(10_000), true);
        final Investor b = Investor.build(a);
        final ExecutorService e = Executors.newFixedThreadPool(1);
        try {
            final StrategyProvider p = mock(StrategyProvider.class);
            final DaemonInvestmentMode d = new DaemonInvestmentMode(t -> {
            }, a, b, p, Duration.ofSeconds(1), Duration.ofSeconds(1));
            final Future<ReturnCode> f = e.submit(() -> d.apply(lifecycle)); // will block
            assertThatThrownBy(() -> f.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
            lifecycle.resumeToShutdown(); // unblock
            assertThat(f.get()).isEqualTo(ReturnCode.OK); // should now finish
            verify(p).getToInvest();
            verify(p).getToPurchase();
        } finally {
            e.shutdownNow();
        }
    }

    @Test
    void nextEvenHourTomorrow() {
        final LocalDate now = LocalDate.now();
        final ZonedDateTime nearlyTomorrow = ZonedDateTime.of(now, LocalTime.of(23, 59, 59), Defaults.ZONE_ID);
        final Duration next = DaemonInvestmentMode.getUntilNextEvenHour(nearlyTomorrow);
        final ZonedDateTime nextEvenHour = nearlyTomorrow.plus(next);
        assertThat(nextEvenHour)
                .isEqualTo(ZonedDateTime.of(now.plusDays(1), LocalTime.of(0, 0, 0), Defaults.ZONE_ID));
    }

    @Test
    void nextEvenHourToday() {
        final LocalDate now = LocalDate.now();
        final ZonedDateTime evenHour = ZonedDateTime.of(now, LocalTime.of(0, 59, 59), Defaults.ZONE_ID);
        final Duration next = DaemonInvestmentMode.getUntilNextEvenHour(evenHour);
        final ZonedDateTime nextEvenHour = evenHour.plus(next);
        assertThat(nextEvenHour)
                .isEqualTo(ZonedDateTime.of(now, LocalTime.of(2, 0, 0), Defaults.ZONE_ID));
    }

    @AfterEach
    void cleanup() {
        final ShutdownHook.Result r = new ShutdownHook.Result(ReturnCode.OK, null);
        lifecycle.getShutdownHooks().forEach(h -> h.get().ifPresent(s -> s.accept(r)));
    }
}
