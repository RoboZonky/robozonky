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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class EventFiringQueueTest extends AbstractRoboZonkyTest {

    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    @AfterEach
    void shutdown() {
        executor.shutdownNow();
    }

    @Test
    void test() {
        final EventFiringQueue q = new EventFiringQueue();
        final Runnable r = mock(Runnable.class);
        final Runnable r2 = mock(Runnable.class);
        q.fire(r);
        final Runnable f = q.fire(r2);
        verify(r, never()).run();
        verify(r2, never()).run();
        new EventFiring(q.getQueue()).run();
        f.run();
        verify(r, times(1)).run();
        verify(r2, times(1)).run();
    }

}
