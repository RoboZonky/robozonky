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

import java.util.ArrayList;
import java.util.List;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.app.events.EventFiringListener;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.events.LazyEvent;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractEventLeveragingTest extends AbstractRoboZonkyTest {

    private final MyEventFiringListener listener = new MyEventFiringListener();

    protected List<Event> getEventsFired() {
        return listener.getEventsFired();
    }

    protected List<Event> getEventsRequested() {
        return listener.getEventsRequested();
    }

    protected List<Event> getEventsQueued() {
        return listener.getEventsQueued();
    }

    protected List<Event> getEventsFailed() {
        return listener.getEventsFailed();
    }

    protected void readPreexistingEvents() {
        listener.clear();
    }

    @BeforeEach
    public void startListeningForEvents() { // initialize session and create a listener
        Events.forSession(SESSION).addListener(listener);
    }

    @AfterEach
    public void stopListeningForEvents() {
        Events.forSession(SESSION).removeListener(listener);
        readPreexistingEvents();
    }

    private static class MyEventFiringListener implements EventFiringListener {

        private final List<Event> eventsFired = new ArrayList<>(0);
        private final List<Event> eventsRequested = new ArrayList<>(0);
        private final List<Event> eventsFailed = new ArrayList<>(0);
        private final List<Event> eventsQueued = new ArrayList<>(0);

        @Override
        public void requested(final LazyEvent<? extends Event> event) {
            eventsRequested.add(event.get());
        }

        @Override
        public <T extends Event> void queued(final T event, final Class<? extends EventListener<T>> listener) {
            eventsQueued.add(event);
        }

        @Override
        public <T extends Event> void fired(final T event, final Class<? extends EventListener<T>> listener) {
            eventsFired.add(event);
        }

        @Override
        public <T extends Event> void failed(final LazyEvent<? extends Event> event,
                                             final Class<? extends EventListener<T>> listener, final Exception ex) {
            eventsFailed.add(event.get());
        }

        public List<Event> getEventsFired() {
            return eventsFired;
        }

        public List<Event> getEventsRequested() {
            return eventsRequested;
        }

        public List<Event> getEventsFailed() {
            return eventsFailed;
        }

        public List<Event> getEventsQueued() {
            return eventsQueued;
        }

        public void clear() {
            eventsFired.clear();
            eventsRequested.clear();
            eventsFailed.clear();
            eventsQueued.clear();
        }
    }
}
