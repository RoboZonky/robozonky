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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class MainControlTest {

    @Test(timeout = 5000)
    public void operation() throws InterruptedException, ExecutionException {
        final ExecutorService e = Executors.newFixedThreadPool(1);
        final MainControl mainControl = new MainControl();
        final AtomicBoolean started = new AtomicBoolean(false);
        final Future<?> f = e.submit(() -> {
            started.set(true);
            try {
                mainControl.waitUntilTriggered();
            } catch (final InterruptedException e1) {
                throw new IllegalStateException(e1);
            }
        });
        while (!started.get()) {
            Thread.sleep(1);
        }
        mainControl.valueUnset(null);
        Assertions.assertThatThrownBy(() -> f.get(1, TimeUnit.SECONDS))
                .isInstanceOf(TimeoutException.class);  // nothing will happen
        final ApiVersion v = Mockito.mock(ApiVersion.class);
        mainControl.valueSet(v);
        f.get(); // make sure task finished
        Assertions.assertThat(mainControl.getApiVersion()).contains(v);
    }
}
