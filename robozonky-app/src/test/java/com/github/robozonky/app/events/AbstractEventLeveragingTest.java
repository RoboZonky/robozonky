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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.app.tenant.PowerTenant;
import com.github.robozonky.common.management.Management;
import com.github.robozonky.common.remote.Zonky;
import com.github.robozonky.common.tenant.LazyEvent;
import com.github.robozonky.test.AbstractRoboZonkyTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;

public abstract class AbstractEventLeveragingTest extends AbstractRoboZonkyTest {

    private final MyEventFiringListener listener = new MyEventFiringListener();

    protected static PowerTenant mockTenant() {
        return mockTenant(harmlessZonky(10_000));
    }

    protected static PowerTenant mockTenant(final Zonky zonky) {
        return mockTenant(zonky, true);
    }

    protected static PowerTenant mockTenant(final Zonky zonky, final boolean isDryRun) {
        return Mockito.spy(new TestingEventTenant(isDryRun ? SESSION_DRY : SESSION, zonky));
    }

    private void waitForEventProcessing() {
        while (!EventFiringQueue.INSTANCE.getQueue().isEmpty()) {
            logger.debug("Sleeping a while to wait for events to be processed.");
            try {
                Thread.sleep(100);
            } catch (final InterruptedException ex) {
                // nothing to do here
            }
        }
    }

    protected List<Event> getEventsFired() {
        waitForEventProcessing();
        return listener.getEventsFired();
    }

    protected List<Event> getEventsRequested() {
        waitForEventProcessing();
        return listener.getEventsRequested();
    }

    protected List<Event> getEventsReady() {
        waitForEventProcessing();
        return listener.getEventsReady();
    }

    protected List<Event> getEventsFailed() {
        waitForEventProcessing();
        return listener.getEventsFailed();
    }

    protected void readPreexistingEvents() {
        waitForEventProcessing();
        listener.clear();
    }

    private final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    @BeforeEach
    public void startListeningForEvents() { // initialize session and create a listener
        final PowerTenant t = mockTenant();
        Events.forSession(t).addListener(listener);
        final Runnable r = () -> new EventFiring().run(); // make sure there is something reading the queue
        service.scheduleAtFixedRate(r, 0, 1, TimeUnit.SECONDS);
    }

    @AfterEach
    private void unregisterBeansCreatedByUs() {
        Management.unregisterAll();
    }

    @AfterEach
    public void stopListeningForEvents() {
        final PowerTenant t = mockTenant();
        Events.forSession(t).removeListener(listener);
        readPreexistingEvents();
        service.shutdown();
    }

    @AfterEach
    public void unregisterAllShutdownHooks() { // so that PITest can shut down all child processes
        Lifecycle.clearShutdownHooks();
    }

    private static class MyEventFiringListener implements EventFiringListener {

        private final List<Event> eventsFired = new ArrayList<>(0);
        private final List<Event> eventsRequested = new ArrayList<>(0);
        private final List<Event> eventsFailed = new ArrayList<>(0);
        private final List<Event> eventsReady = new ArrayList<>(0);

        @Override
        public void requested(final LazyEvent<? extends Event> event) {
            eventsRequested.add(event.get());
        }

        @Override
        public <T extends Event> void ready(final T event, final Class<? extends EventListener<T>> listener) {
            eventsReady.add(event);
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

        public List<Event> getEventsReady() {
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
