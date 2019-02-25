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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionalInstanceStateTest extends AbstractRoboZonkyTest {

    @Test
    void delegatesKeys() {
        final InstanceState<String> parent = mock(InstanceState.class);
        when(parent.getKeys()).thenReturn(Stream.empty());
        final TransactionalInstanceState<String> s = new TransactionalInstanceState<>(Collections.emptyList(), parent);
        assertThat(s.getKeys()).isSameAs(parent.getKeys());
    }

    @Test
    void delegatesValues() {
        final String key = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();
        final InstanceState<String> parent = mock(InstanceState.class);
        when(parent.getValue(key)).thenReturn(Optional.of(value));
        final TransactionalInstanceState<String> s = new TransactionalInstanceState<>(Collections.emptyList(), parent);
        assertThat(s.getValue(key)).contains(value);
    }

    @Test
    void addsUpdate() {
        final List<Runnable> items = new ArrayList<>();
        final InstanceState<String> parent = mock(InstanceState.class);
        final TransactionalInstanceState<String> s = new TransactionalInstanceState<>(items, parent);
        s.update(m -> {});
        verify(parent, never()).update(any());
        items.get(0).run();
        verify(parent).update(any());
    }

    @Test
    void addsReset() {
        final List<Runnable> items = new ArrayList<>();
        final InstanceState<String> parent = mock(InstanceState.class);
        final TransactionalInstanceState<String> s = new TransactionalInstanceState<>(items, parent);
        s.reset(m -> {});
        verify(parent, never()).reset(any());
        items.get(0).run();
        verify(parent).reset(any());
    }

}
