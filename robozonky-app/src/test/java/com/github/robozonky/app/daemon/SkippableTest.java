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

import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyDaemonFailedEvent;
import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.app.tenant.PowerTenant;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SkippableTest extends AbstractZonkyLeveragingTest {

    @Test
    void skips() {
        final Runnable r = mock(Runnable.class);
        final PowerTenant t = mockTenant();
        when(t.isAvailable(any())).thenReturn(false);
        final Consumer<Throwable> c = mock(Consumer.class);
        final Skippable s = new Skippable(r, t);
        s.run();
        verify(r, never()).run();
        verify(c, never()).accept(any());
        assertThat(this.getEventsRequested()).isEmpty();
    }

    @Test
    void doesNotSkip() {
        final Runnable r = mock(Runnable.class);
        final PowerTenant t = mockTenant();
        final Consumer<Throwable> c = mock(Consumer.class);
        final Skippable s = new Skippable(r, t);
        s.run();
        verify(r).run();
        verify(c, never()).accept(any());
        assertThat(this.getEventsRequested()).isEmpty();
    }

    @Test
    void fails() {
        final Runnable r = mock(Runnable.class);
        doThrow(IllegalStateException.class).when(r).run();
        final PowerTenant t = mockTenant();
        final Consumer<Throwable> c = mock(Consumer.class);
        final Skippable s = new Skippable(r, t);
        s.run();
        verify(c, never()).accept(any());
        assertThat(this.getEventsRequested()).hasSize(1)
                .first()
                .isInstanceOf(RoboZonkyDaemonFailedEvent.class);
    }

}

