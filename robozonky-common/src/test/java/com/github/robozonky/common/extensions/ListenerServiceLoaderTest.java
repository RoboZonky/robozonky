/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.common.extensions;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.api.notifications.RoboZonkyStartingEvent;
import com.github.robozonky.api.notifications.RoboZonkyTestingEvent;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ListenerServiceLoaderTest {

    @Test
    void correctLoading() {
        final RoboZonkyStartingEvent e = new RoboZonkyStartingEvent();
        final EventListener<RoboZonkyStartingEvent> l = mock(EventListener.class);
        final ListenerService s1 = mock(ListenerService.class);
        final EventListenerSupplier<RoboZonkyStartingEvent> returned = () -> Optional.of(l);
        doReturn(returned).when(s1).findListeners(eq(e.getClass()));
        final ListenerService s2 = mock(ListenerService.class);
        doReturn((EventListenerSupplier<RoboZonkyStartingEvent>) Optional::empty)
                .when(s2).findListeners(eq(e.getClass()));
        final Iterable<ListenerService> s = () -> Arrays.asList(s1, s2).iterator();
        final List<EventListenerSupplier<RoboZonkyStartingEvent>> r =
                ListenerServiceLoader.load(RoboZonkyStartingEvent.class, s);
        assertThat(r).hasSize(2);
        assertThat(r)
                .first()
                .has(new Condition<>(result -> result.get().isPresent() && result.get().get() == l,
                                     "Exists"));
        assertThat(r)
                .last()
                .has(new Condition<>(result -> !result.get().isPresent(), "Does not exist"));
    }

    @Test
    void empty() {
        final List<EventListenerSupplier<RoboZonkyTestingEvent>> r =
                ListenerServiceLoader.load(RoboZonkyTestingEvent.class);
        assertThat(r).isEmpty(); // no providers registered by default
    }
}
