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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.daemon.operations.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.common.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
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
            final DaemonInvestmentMode d = spy(new DaemonInvestmentMode("", t -> {
            }, a, b, p, Duration.ofSeconds(1), Duration.ofSeconds(1)));
            doNothing().when(d).scheduleJob(any(), any()); // otherwise jobs will run, which may try to log into Zonky
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

    @AfterEach
    void cleanup() {
        final ShutdownHook.Result r = new ShutdownHook.Result(ReturnCode.OK, null);
        lifecycle.getShutdownHooks().forEach(h -> h.get().ifPresent(s -> s.accept(r)));
    }
}
