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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.api.ReturnCode;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.common.remote.Zonky;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DaemonInvestmentModeTest extends AbstractZonkyLeveragingTest {

    private final Lifecycle lifecycle = new Lifecycle();

    @Test
    void get() throws Exception {
        final Authenticated a = mockAuthentication(mock(Zonky.class));
        final Investor.Builder b = new Investor.Builder().asDryRun();
        final ExecutorService e = Executors.newFixedThreadPool(1);
        try {
            final DaemonInvestmentMode d = new DaemonInvestmentMode(t -> {
            }, a, b, mock(StrategyProvider.class), Duration.ofSeconds(1), Duration.ofSeconds(1));
            final Future<ReturnCode> f = e.submit(() -> d.apply(lifecycle)); // will block
            assertThatThrownBy(() -> f.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
            lifecycle.resumeToShutdown(); // unblock
            assertThat(f.get()).isEqualTo(ReturnCode.OK); // should now finish
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
