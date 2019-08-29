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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.internal.jobs.SimplePayload;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DaemonTest extends AbstractZonkyLeveragingTest {

    private final Lifecycle lifecycle = new Lifecycle();

    @Test
    void get() throws Exception {
        final PowerTenant a = mockTenant(harmlessZonky(), true);
        final ExecutorService e = Executors.newFixedThreadPool(1);
        try (final Daemon d = spy(new Daemon(a, lifecycle))) {
            assertThat(d.getSessionInfo()).isSameAs(a.getSessionInfo());
            doNothing().when(d).submitWithTenant(any(), any(), any(), any(), any(), any());
            doNothing().when(d).submitTenantless(any(), any(), any(), any(), any(), any());
            final Future<ReturnCode> f = e.submit(d::get); // will block
            assertThatThrownBy(() -> f.get(1, TimeUnit.SECONDS)).isInstanceOf(TimeoutException.class);
            lifecycle.resumeToShutdown(); // unblock
            assertThat(f.get()).isEqualTo(ReturnCode.OK); // should now finish
            // call all the jobs and daemons we know about
            verify(d, times(2)).submitTenantless(any(), any(SimplePayload.class), any(), any(), any(), any());
            verify(d, times(9)).submitWithTenant(any(), any(), any(), any(), any(), any());
        } finally {
            e.shutdownNow();
        }
        verify(a).close();
    }
}
