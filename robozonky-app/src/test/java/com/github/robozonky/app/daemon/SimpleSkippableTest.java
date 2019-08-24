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

package com.github.robozonky.app.daemon;

import java.util.function.Consumer;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SimpleSkippableTest extends AbstractZonkyLeveragingTest {

    @Test
    void fails() {
        final Runnable r = mock(Runnable.class);
        doThrow(IllegalStateException.class).when(r).run();
        final Runnable s = new SimpleSkippable(r);
        s.run();
        assertThat(this.getEventsRequested()).isEmpty();
    }

    @Test
    void dies() {
        final Runnable r = mock(Runnable.class);
        doThrow(OutOfMemoryError.class).when(r).run();
        final Consumer<Throwable> c = mock(Consumer.class);
        final Runnable s = new SimpleSkippable(r, c);
        assertThatThrownBy(s::run).isInstanceOf(OutOfMemoryError.class);
        verify(c, only()).accept(any());
    }

}

