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

package com.github.robozonky.app;

import java.util.Optional;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ShutdownHookTest {

    @Test
    void noShutdownHandler() {
        final ShutdownHook.Handler h = mock(ShutdownHook.Handler.class);
        when(h.get()).thenReturn(Optional.empty());
        final ShutdownHook s = new ShutdownHook();
        assertThat(s.register(h)).isFalse();
        try {
            s.execute(new ShutdownHook.Result(ReturnCode.OK, null));
        } catch (final RuntimeException ex) {
            fail("Should not have been thrown.", ex);
        }
    }

    @Test
    void exceptionHandlingOnRegistration() {
        final ShutdownHook.Handler h = mock(ShutdownHook.Handler.class);
        doThrow(new IllegalStateException("Testing exception")).when(h).get();
        assertThat(new ShutdownHook().register(h)).isFalse();
    }

    @Test
    void nullHandler() {
        assertThatThrownBy(() -> new ShutdownHook().register(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void exceptionHandlingOnShutdown() {
        final ShutdownHook.Handler h = mock(ShutdownHook.Handler.class);
        when(h.get()).thenReturn(Optional.of(code -> {
            throw new IllegalStateException("Testing exception.");
        }));
        final ShutdownHook s = new ShutdownHook();
        assertThat(s.register(h)).isTrue();
        try {
            s.execute(new ShutdownHook.Result(ReturnCode.OK, null));
        } catch (final RuntimeException ex) {
            fail("Should not have been thrown.", ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    void proper() {
        final Consumer<ShutdownHook.Result> c = mock(Consumer.class);
        final ShutdownHook.Handler h = mock(ShutdownHook.Handler.class);
        when(h.get()).thenReturn(Optional.of(c));
        final ShutdownHook s = new ShutdownHook();
        assertThat(s.register(h)).isTrue();
        final ShutdownHook.Result r = new ShutdownHook.Result(ReturnCode.OK, null);
        s.execute(r);
        verify(c).accept(eq(r));
    }
}
