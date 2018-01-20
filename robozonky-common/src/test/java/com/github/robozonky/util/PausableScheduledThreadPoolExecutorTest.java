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

package com.github.robozonky.util;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class PausableScheduledThreadPoolExecutorTest {

    @Test
    public void pausing() throws InterruptedException, TimeoutException, ExecutionException {
        final PausableScheduledExecutorService e = new PausableScheduledThreadPoolExecutor(1);
        e.pause();
        final Runnable r = () -> {
            // no need to do anything
        };
        final Future<?> f = e.schedule(r, 0, TimeUnit.SECONDS);
        Assertions.assertThatThrownBy(() -> {
            f.get(1, TimeUnit.SECONDS); // make sure nothing was run, meaning that the task was queued
        }).isInstanceOf(TimeoutException.class);
        e.resume();
        f.get(1, TimeUnit.SECONDS); // now make sure task was run
    }
}
