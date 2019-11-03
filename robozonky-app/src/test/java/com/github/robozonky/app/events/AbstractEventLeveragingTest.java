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

package com.github.robozonky.app.events;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.app.tenant.TestingPowerTenant;
import com.github.robozonky.internal.management.Management;
import com.github.robozonky.internal.remote.Zonky;
import com.github.robozonky.internal.tenant.LazyEvent;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public abstract class AbstractEventLeveragingTest extends AbstractRoboZonkyTest {

    private final MyEventFiringListener listener = new MyEventFiringListener();

    protected static PowerTenant mockTenant() {
        return mockTenant(harmlessZonky());
    }

    protected static PowerTenant mockTenant(final Zonky zonky) {
        return mockTenant(zonky, true);
    }

    protected static PowerTenant mockTenant(final Zonky zonky, final boolean isDryRun) {
        return Mockito.spy(new TestingPowerTenant(isDryRun ? SESSION_DRY : SESSION, zonky));
    }

    protected List<Event> getEventsFired() {
        awaitTerminationOfParallelTasks();
        return listener.getEventsFired();
    }

    protected List<Event> getEventsRequested() {
        awaitTerminationOfParallelTasks();
        return listener.getEventsRequested();
    }

    protected List<Event> getEventsReady() {
        awaitTerminationOfParallelTasks();
        return listener.getEventsReady();
    }

    protected List<Event> getEventsFailed() {
        awaitTerminationOfParallelTasks();
        return listener.getEventsFailed();
    }

    protected void readPreexistingEvents() {
        awaitTerminationOfParallelTasks();
        listener.clear();
    }

    @AfterEach
    private void unregisterBeansCreatedByUs() {
        Management.unregisterAll();
    }

    @AfterEach
    public void unregisterAllShutdownHooks() { // so that PITest can shut down all child processes
        Lifecycle.getShutdownHooks().forEach(h -> Runtime.getRuntime().removeShutdownHook(h));
        Thread.setDefaultUncaughtExceptionHandler(null);
    }

    /**
     * Heavily synchronized as it has been shown to improve stability of tests in exactly the same way that just
     * converting the {@link ArrayList}s to {@link CopyOnWriteArrayList} did not.
     */
    private static class MyEventFiringListener implements EventFiringListener {

        private final List<Event> eventsFired = new ArrayList<>(0);
        private final List<Event> eventsRequested = new ArrayList<>(0);
        private final List<Event> eventsFailed = new ArrayList<>(0);
        private final List<Event> eventsReady = new ArrayList<>(0);

        @Override
        public synchronized void requested(final LazyEvent<? extends Event> event) {
            eventsRequested.add(event.get());
        }

        @Override
        public synchronized <T extends Event> void ready(final T event, final Class<? extends EventListener<T>> listener) {
            eventsReady.add(event);
        }

        @Override
        public synchronized <T extends Event> void fired(final T event, final Class<? extends EventListener<T>> listener) {
            eventsFired.add(event);
        }

        @Override
        public synchronized <T extends Event> void failed(final LazyEvent<? extends Event> event,
                                             final Class<? extends EventListener<T>> listener, final Exception ex) {
            eventsFailed.add(event.get());
        }

        public synchronized List<Event> getEventsFired() {
            return eventsFired;
        }

        public synchronized List<Event> getEventsRequested() {
            return eventsRequested;
        }

        public synchronized List<Event> getEventsFailed() {
            return eventsFailed;
        }

        public synchronized List<Event> getEventsReady() {
            return eventsReady;
        }

        public void clear() {
            eventsFired.clear();
            eventsRequested.clear();
            eventsFailed.clear();
            eventsReady.clear();
        }
    }
}
