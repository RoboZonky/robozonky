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

import java.util.concurrent.Semaphore;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Assume;
import org.junit.Test;

public class SuddenDeathWorkaroundTest {

    @Test
    public void detection() throws InterruptedException {
        final Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();
        Assume.assumeTrue(semaphore.availablePermits() == 0);
        final SuddenDeathWorkaround workaround = new SuddenDeathWorkaround(semaphore, 1);
        Assertions.assertThat(workaround.isSuddenDeath()).isFalse();
        workaround.run();
        Assertions.assertThat(workaround.isSuddenDeath()).isFalse();
        workaround.run();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(workaround.isSuddenDeath()).isTrue();
            softly.assertThat(semaphore.availablePermits()).isGreaterThan(0);
        });
    }

    @Test
    public void reset() throws InterruptedException {
        final Semaphore semaphore = new Semaphore(1);
        semaphore.acquire();
        Assume.assumeTrue(semaphore.availablePermits() == 0);
        final SuddenDeathWorkaround workaround = new SuddenDeathWorkaround(semaphore, 1);
        Assertions.assertThat(workaround.isSuddenDeath()).isFalse();
        workaround.run();
        Assertions.assertThat(workaround.isSuddenDeath()).isFalse();
        workaround.registerMarketplaceCheck();
        workaround.run();
        Assertions.assertThat(workaround.isSuddenDeath()).isFalse();
    }

}
