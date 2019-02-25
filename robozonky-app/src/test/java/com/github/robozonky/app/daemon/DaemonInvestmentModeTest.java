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

package com.github.robozonky.app.daemon;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.jobs.SimplePayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DaemonInvestmentModeTest extends AbstractZonkyLeveragingTest {

    private static final Duration ONE_SECOND = Duration.ofSeconds(1);

    private final Lifecycle lifecycle = new Lifecycle();

    @Test
    void get() throws Exception {
        final PowerTenant a = mockTenant(harmlessZonky(10_000), true);
        final Investor b = Investor.build(a);
        final ExecutorService e = Executors.newFixedThreadPool(1);
        try (final DaemonInvestmentMode d = spy(new DaemonInvestmentMode(a, b, ONE_SECOND))) {
            assertThat(d.getSessionInfo()).isSameAs(a.getSessionInfo());
            doNothing().when(d).submit(any(), any(), any(), any(), any());
            final Future<ReturnCode> f = e.submit(() -> d.apply(lifecycle)); // will block
            assertThatThrownBy(() -> f.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
            lifecycle.resume(); // unblock
            assertThat(f.get()).isEqualTo(ReturnCode.OK); // should now finish
            // call all the jobs and daemons we know about
            verify(d, times(2)).submit(any(), any(SimplePayload.class), any(), any(), any());
            verify(d, times(11)).submit(any(), any(), any(), any(), any());
        } finally {
            e.shutdownNow();
        }
        verify(a).close();
    }
}
