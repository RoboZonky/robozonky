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

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.RoboZonkyStartingEvent;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class EventsTest extends AbstractEventLeveragingRoboZonkyTest {

    @Test
    public void firingAndFailing() {
        final EventListener<RoboZonkyStartingEvent> listener = Mockito.mock(EventListener.class);
        final Refreshable<EventListener<RoboZonkyStartingEvent>> r = Refreshable.createImmutable(listener);
        r.run();
        Mockito.doThrow(RuntimeException.class).when(listener).handle(ArgumentMatchers.any(), ArgumentMatchers.any());
        Events.INSTANCE.loadListeners(RoboZonkyStartingEvent.class, r);
        final RoboZonkyStartingEvent e = new RoboZonkyStartingEvent();
        Events.fire(e);
        Assertions.assertThat(Events.getFired()).contains(e);
        Mockito.verify(listener).handle(ArgumentMatchers.eq(e), ArgumentMatchers.any());
    }
}
