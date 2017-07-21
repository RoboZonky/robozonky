/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import com.github.triceo.robozonky.app.ShutdownEnabler;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Test;

public class DaemonShutdownHookTest {

    private final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    @Test
    public void check() throws InterruptedException, ExecutionException {
        final CountDownLatch l = new CountDownLatch(1);
        final Future<?> f = EXECUTOR.submit(new DaemonShutdownHook(l));
        l.await();
        ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.get().countDown();
        f.get();
        Assertions.assertThat(f).isDone();
    }

    @After
    public void cleanup() {
        EXECUTOR.shutdownNow();
        ShutdownEnabler.DAEMON_ALLOWED_TO_TERMINATE.set(new CountDownLatch(1));
    }
}
