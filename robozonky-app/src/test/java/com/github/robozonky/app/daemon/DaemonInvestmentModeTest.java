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
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.daemon.operations.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.common.Tenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

class DaemonInvestmentModeTest extends AbstractZonkyLeveragingTest {

    private static final Duration ONE_SECOND = Duration.ofSeconds(1);

    private final Lifecycle lifecycle = new Lifecycle();

    @Test
    void get() throws Exception {
        final Tenant a = mockTenant(harmlessZonky(10_000), true);
        final Investor b = Investor.build(a);
        final ExecutorService e = Executors.newFixedThreadPool(1);
        final StrategyProvider p = mock(StrategyProvider.class);
        final String name = UUID.randomUUID().toString();
        try (final DaemonInvestmentMode d = spy(new DaemonInvestmentMode(name, a, b, p, ONE_SECOND, ONE_SECOND))) {
            assertThat(d.getSessionName()).isEqualTo(name);
            doNothing().when(d).scheduleJob(any(), any(), any()); // otherwise jobs will run and try to log into Zonky
            final Future<ReturnCode> f = e.submit(() -> d.apply(lifecycle)); // will block
            assertThatThrownBy(() -> f.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
            lifecycle.resumeToShutdown(); // unblock
            assertThat(f.get()).isEqualTo(ReturnCode.OK); // should now finish
            verify(p).getToInvest();
            verify(p).getToPurchase();
        } finally {
            e.shutdownNow();
        }
        verify(a).close();
    }

}
