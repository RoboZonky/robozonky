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

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class EventFiringQueueTest {

    @Test
    void test() {
        final AtomicReference<EventFiringRunnable> currentRunnable = new AtomicReference<>();
        final Function<BlockingQueue<Runnable>, EventFiringRunnable> supplier = q -> {
            final EventFiringRunnable r = new EventFiringRunnable(q);
            currentRunnable.set(r);
            return r;
        };
        final EventFiringQueue q = new EventFiringQueue(supplier);
        final Runnable r = mock(Runnable.class);
        final Runnable r2 = mock(Runnable.class);
        q.fire(r);
        q.fire(r2).join();
        verify(r, times(1)).run();
        verify(r2, times(1)).run();
    }

}
