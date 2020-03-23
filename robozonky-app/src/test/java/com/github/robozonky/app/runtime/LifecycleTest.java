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
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.notifications.RoboZonkyCrashedEvent;
import com.github.robozonky.app.ShutdownHook;
import com.github.robozonky.app.events.AbstractEventLeveragingTest;

class LifecycleTest extends AbstractEventLeveragingTest {

    @Test
    void create() {
        final ShutdownHook hooks = spy(ShutdownHook.class);
        new Lifecycle(hooks);
        verify(hooks).register(any()); // 2 shutdown hooks have been registered
    }

    @Test
    void suspendAndResumeToFail() throws InterruptedException {
        assertThat(Thread.getDefaultUncaughtExceptionHandler()).isNull();
        final CountDownLatch cdl = mock(CountDownLatch.class);
        doThrow(InterruptedException.class).when(cdl)
            .await();
        final Lifecycle c = new Lifecycle(cdl, new ShutdownHook());
        assertThat(c.isFailed()).isFalse();
        c.suspend();
        verify(cdl).countDown();
        assertSoftly(softly -> {
            softly.assertThat(Lifecycle.countShutdownHooks())
                .isEqualTo(1);
            softly.assertThat(Thread.getDefaultUncaughtExceptionHandler())
                .isNotNull();
            softly.assertThat(c.isFailed())
                .isTrue();
            softly.assertThat(getEventsRequested())
                .hasSize(1)
                .first()
                .isInstanceOf(RoboZonkyCrashedEvent.class);
        });
    }
}
