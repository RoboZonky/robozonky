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

package com.github.robozonky.app.events;

import java.time.Duration;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingDeque;

import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.Mockito.*;

class EventFiringTest extends AbstractRoboZonkyTest {

    @Test
    void endsLoop() throws InterruptedException {
        final Runnable first = mock(Runnable.class);
        final Runnable failing = mock(Runnable.class);
        doAnswer(i -> {
            throw new InterruptedException();
        }).when(failing).run();
        final BlockingQueue<Runnable> q = new LinkedBlockingDeque<>();
        q.put(first);
        q.put(failing);
        final Runnable last = mock(Runnable.class);
        q.put(last);
        // will interrupt current thread, therefore run in on a different thread than this
        final CompletableFuture<Void> task = CompletableFuture.runAsync(() -> {
            final EventFiring e = new EventFiring(q);
            e.run();
        });
        assertTimeout(Duration.ofSeconds(5), task::join); // make sure it's finished
        assertThat(task).isCompleted();
        verify(first).run();
        verify(failing).run();
        verify(last, never()).run();
    }
}
