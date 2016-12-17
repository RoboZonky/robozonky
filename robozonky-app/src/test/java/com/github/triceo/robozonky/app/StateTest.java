/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.util.Optional;

import com.github.triceo.robozonky.api.ReturnCode;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class StateTest {

    @Test
    public void noShutdownHandler() {
        final State.Handler h = Mockito.mock(State.Handler.class);
        Mockito.when(h.get()).thenReturn(Optional.empty());
        final State s = new State();
        Assertions.assertThat(s.register(h)).isFalse();
        try {
            s.shutdown(ReturnCode.OK);
        } catch (final RuntimeException ex) {
            Assertions.fail("Should not have been thrown.", ex);
        }
    }

    @Test
    public void exceptionHandlingOnRegistration() {
        final State.Handler h = Mockito.mock(State.Handler.class);
        Mockito.doThrow(new IllegalStateException("Testing exception")).when(h).get();
        Assertions.assertThat(new State().register(h)).isFalse();
    }

    @Test
    public void exceptionHandlingOnShutdown() {
        final State.Handler h = Mockito.mock(State.Handler.class);
        Mockito.when(h.get()).thenReturn(Optional.of(code -> {
            throw new IllegalStateException("Testing exception.");
        }));
        final State s = new State();
        Assertions.assertThat(s.register(h)).isTrue();
        try {
            s.shutdown(ReturnCode.OK);
        } catch (final RuntimeException ex) {
            Assertions.fail("Should not have been thrown.", ex);
        }
    }
}
