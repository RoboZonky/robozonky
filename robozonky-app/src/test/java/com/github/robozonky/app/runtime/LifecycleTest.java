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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import com.github.robozonky.app.ShutdownHook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.SoftAssertions.*;

class LifecycleTest {

    private final ExecutorService e = Executors.newFixedThreadPool(1);

    @AfterEach
    void shutdown() {
        e.shutdownNow();
    }

    @Test
    void failing() {
        final Throwable t = new IllegalStateException("Testing exception.");
        final CountDownLatch c = new CountDownLatch(1);
        final Lifecycle h = new Lifecycle(c);
        Assertions.assertTimeout(Duration.ofSeconds(1), () -> h.resumeToFail(t));
        assertSoftly(softly -> {
            softly.assertThat(h.getTerminationCause()).contains(t);
            softly.assertThat(h.getZonkyApiVersion()).isEmpty();
            softly.assertThat(c.getCount()).isEqualTo(0);
        });
    }

    @Test
    void create() {
        final Lifecycle h = new Lifecycle();
        assertSoftly(softly -> {
            softly.assertThat(h.getTerminationCause()).isEmpty();
            softly.assertThat(h.getShutdownHooks())
                    .hasSize(2)
                    .hasOnlyElementsOfType(ShutdownHook.Handler.class);
        });
    }
}
