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

package com.github.robozonky.app;

import java.util.Optional;

import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.RoboZonkyStartingEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class EventsTest extends AbstractEventLeveragingTest {

    @Mock
    private EventListener<RoboZonkyStartingEvent> listener;

    @Test
    void firingAndFailing() {
        final EventListenerSupplier<RoboZonkyStartingEvent> r = () -> Optional.of(listener);
        doThrow(RuntimeException.class).when(listener).handle(any(), any());
        final Events.EventSpecific<RoboZonkyStartingEvent> event =
                Events.INSTANCE.loadListeners(RoboZonkyStartingEvent.class, r);
        assertThat(event).isNotNull();
        final RoboZonkyStartingEvent e = new RoboZonkyStartingEvent();
        Events.fire(e);
        assertThat(Events.getFired()).contains(e);
        verify(listener).handle(eq(e), any());
    }
}
