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

package com.github.robozonky.app.runtime;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.events.AbstractEventLeveragingTest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class LifecycleTest extends AbstractEventLeveragingTest {

    @Test
    void waitUntilOnline() throws ExecutionException, InterruptedException, TimeoutException {
        final ExecutorService e = Executors.newCachedThreadPool();
        try {
            final MainControl c = new MainControl();
            final Future<Boolean> f = e.submit(() -> Lifecycle.waitUntilOnline(c));
            Assertions.assertThrows(TimeoutException.class, () -> f.get(1, TimeUnit.SECONDS)); // we are blocked
            c.valueSet("");
            assertThat(f.get(1, TimeUnit.SECONDS)).isTrue(); // this will return now
            assertThat(f).isDone();
        } finally {
            e.shutdownNow();
        }
    }

    @Test
    void waitUntilOnlineInterrupted() throws InterruptedException {
        final ExecutorService e = Executors.newCachedThreadPool();
        try {
            final MainControl mc = mock(MainControl.class);
            doThrow(InterruptedException.class).when(mc).waitUntilTriggered();
            final Lifecycle c = new Lifecycle(mc);
            final boolean result = c.waitUntilOnline();
            assertThat(result).isFalse();
        } finally {
            e.shutdownNow();
        }
    }

    /**
     * This will unfortunately call the Zonky API, making the test flaky during Zonky downtimes.
     */
    @Test
    void create() {
        final ShutdownHook hooks = spy(ShutdownHook.class);
        final Lifecycle h = new Lifecycle(hooks);
        assertSoftly(softly -> {
            softly.assertThat(h.getZonkyApiVersion()).isNotEmpty();
            softly.assertThat(h.getZonkyApiLastUpdate()).isNotNull();
            softly.assertThat(h.isOnline()).isTrue();
        });
        verify(hooks).register(any()); // 2 shutdown hooks have been registered
    }
}
