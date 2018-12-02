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

package com.github.robozonky.app.runtime;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.github.robozonky.app.ShutdownHook;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class LifecycleTest {

    @Test
    void failing() {
        final Throwable t = new IllegalStateException("Testing exception.");
        final CountDownLatch c = new CountDownLatch(1);
        final Lifecycle h = new Lifecycle(c);
        Assertions.assertTimeout(Duration.ofSeconds(1), () -> h.resumeToFail(t));
        assertSoftly(softly -> {
            softly.assertThat(h.getTerminationCause()).contains(t);
            softly.assertThat(c.getCount()).isEqualTo(0);
        });
    }

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

    @Test
    void create() {
        final ShutdownHook hooks = spy(ShutdownHook.class);
        final Lifecycle h = new Lifecycle(hooks);
        assertSoftly(softly -> {
            softly.assertThat(h.getZonkyApiVersion()).isEmpty();
            softly.assertThat(h.getTerminationCause()).isEmpty();
        });
        verify(hooks, times(2)).register(any()); // 2 shutdown hooks have been registered
    }
}
