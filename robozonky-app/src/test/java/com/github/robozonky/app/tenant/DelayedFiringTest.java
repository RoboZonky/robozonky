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

package com.github.robozonky.app.tenant;

import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class DelayedFiringTest extends AbstractRoboZonkyTest {

    private static void assertCancelled(final DelayedFiring d) {
        assertThatThrownBy(d::run).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(d::cancel).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> d.delay(mock(Runnable.class))).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void runningDisablesInstance() {
        final DelayedFiring d = new DelayedFiring();
        d.run();
        assertCancelled(d);
    }

    @Test
    void cancellationDisablesInstance() {
        final DelayedFiring d = new DelayedFiring();
        d.delay(mock(Runnable.class));
        assertThat(d.isPending()).isTrue();
        d.cancel();
        assertThat(d.isPending()).isFalse();
        assertCancelled(d);
    }

}
