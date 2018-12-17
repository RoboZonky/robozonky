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

package com.github.robozonky.app.delinquencies;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.GlobalEvent;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.events.GlobalEvents;
import com.github.robozonky.app.events.LazyEvent;
import com.github.robozonky.app.events.SessionEvents;
import com.github.robozonky.common.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates to state, which happen through {@link #getTenant()}, are postponed until {@link #run()} is called. Likewise
 * for events fired through {@link #fire(Event)}.
 * <p>
 * This class is thread-safe, since multiple threads may want to fire events and/or store state data at the same time.
 */
class Transactional implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Transactional.class);

    private final Tenant tenant;
    private final Queue<Object> eventsToFire = new ConcurrentLinkedQueue<>();
    private final Queue<Runnable> stateUpdates = new ConcurrentLinkedQueue<>();

    public Transactional(final Tenant tenant) {
        this.tenant = new TransactionalTenant(this, tenant);
    }

    Queue<Runnable> getStateUpdates() {
        return stateUpdates;
    }

    /**
     * Returns {@link TransactionalTenant} instead of the default implementation.
     * @return
     */
    public Tenant getTenant() {
        return tenant;
    }

    /**
     * Stores event for future firing when {@link #run()}  is called.
     * @param event
     */
    public void fire(final Event event) {
        LOGGER.trace("Event stored within transaction: {}.", event);
        eventsToFire.add(event);
    }

    public void fire(final LazyEvent<? extends Event> event) {
        LOGGER.trace("Lazy event stored within transaction: {}.", event);
        eventsToFire.add(event);
    }

    /**
     * Fire events and update state. Clears internal state, so that the next {@link #run()} call would not do anything
     * unless {@link #fire(Event)} or state updates are performed inbetween.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        LOGGER.debug("Replaying transaction.");
        while (!stateUpdates.isEmpty()) {
            stateUpdates.poll().run();
        }
        final SessionEvents sessionEvents = Events.forSession(getTenant().getSessionInfo());
        final GlobalEvents globalEvents = Events.global();
        while (!eventsToFire.isEmpty()) {
            final Object evt = eventsToFire.poll();
            if (evt instanceof LazyEvent) {
                final LazyEvent<?> event = (LazyEvent<?>)evt;
                if (event.getEventType().isAssignableFrom(GlobalEvent.class)) {
                    globalEvents.fire((LazyEvent<GlobalEvent>)event);
                } else {
                    sessionEvents.fire((LazyEvent<SessionEvent>)event);
                }
            } else if (evt instanceof GlobalEvent) {
                globalEvents.fire((GlobalEvent) evt);
            } else if (evt instanceof SessionEvent) {
                sessionEvents.fire((SessionEvent) evt);
            } else {
                throw new IllegalStateException("Can not happen.");
            }
        }
    }
}
