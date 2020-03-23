/*
 * Copyright 2020 The RoboZonky Project
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

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.test.AbstractRoboZonkyTest;

class ShutdownEnablerTest extends AbstractRoboZonkyTest {

    @Test
    void standard() {
        final ShutdownEnabler se = new ShutdownEnabler();
        final ExecutorService e = Executors.newFixedThreadPool(1);
        final Future<?> f = e.submit(se::waitUntilTriggered);
        assertThatThrownBy(() -> f.get(1, TimeUnit.SECONDS))
            .isInstanceOf(TimeoutException.class); // the thread is blocked
        final Consumer<ReturnCode> c = se.get()
            .orElseThrow(() -> new IllegalStateException("Should have returned."));
        c.accept(ReturnCode.OK); // this unblocks the thread
        // this should return
        assertTimeout(Duration.ofSeconds(5), (Executable) f::get);
        assertThat(f).isDone();
        e.shutdownNow();
    }
}
