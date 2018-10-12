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

package com.github.robozonky.app.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SessionSpecificEventsImpl implements SessionSpecificEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionSpecificEventsImpl.class);
    private static final Map<String, SessionSpecificEventsImpl> BY_TENANT = new ConcurrentHashMap<>(0);
    private final Map<Class<?>, List<EventListenerSupplier<? extends Event>>> suppliers = new ConcurrentHashMap<>(0);
    private final Set<EventFiringListener> listeners = new LinkedHashSet<>(0);
    private final SessionInfo sessionInfo;

    private SessionSpecificEventsImpl(final SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
        addListener(new MyEventFiringListener(sessionInfo));
    }

    static Collection<SessionSpecificEventsImpl> all() { // defensive copy
        return Collections.unmodifiableCollection(new ArrayList<>(BY_TENANT.values()));
    }

    static SessionSpecificEventsImpl forSession(final SessionInfo sessionInfo) {
        return BY_TENANT.computeIfAbsent(sessionInfo.getUsername(), i -> new SessionSpecificEventsImpl(sessionInfo));
    }

    @SuppressWarnings("unchecked")
    static <T extends Event> Class<T> getImplementingEvent(final Class<T> original) {
        final Stream<Class<?>> provided = ClassUtils.getAllInterfaces(original).stream();
        final Stream<Class<?>> interfaces = original.isInterface() ? // interface could be extending it directly
                Stream.concat(Stream.of(original), provided) :
                provided;
        return (Class<T>) interfaces.filter(
                i -> Objects.equals(i.getPackage().getName(), "com.github.robozonky.api.notifications"))
                .filter(i -> i.getSimpleName().endsWith("Event"))
                .filter(i -> !Objects.equals(i.getSimpleName(), "Event"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not an event:" + original));
    }

    private void fire(final LazyEvent<? extends Event> lazyEvent, final EventListener<Event> listener) {
        final Event event = lazyEvent.get(); // possibly incurring performance penalties
        try {
            listeners.forEach(l -> l.queued(event));
            LOGGER.trace("Sending {} to listener {} for {}.", event, listener, sessionInfo);
            listener.handle(event, sessionInfo);
            listeners.forEach(l -> l.fired(event));
        } catch (final RuntimeException ex) {
            listeners.forEach(l -> l.failed(event, ex));
        } finally {
            LOGGER.trace("Fired.");
        }
    }

    @Override
    public boolean addListener(final EventFiringListener listener) {
        LOGGER.debug("Adding listener {} for {}.", listener, sessionInfo);
        return listeners.add(listener);
    }

    @Override
    public boolean removeListener(final EventFiringListener listener) {
        LOGGER.debug("Removing listener {} for {}.", listener, sessionInfo);
        return listeners.remove(listener);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void fire(final Event event) {
        fire(new LazyEventImpl<>((Class<Event>) event.getClass(), () -> event));
    }

    @SuppressWarnings("unchecked")
    @Override
    public void fire(final LazyEvent<? extends Event> event) {
        // loan all listeners
        listeners.forEach(l -> l.requested(event));
        final List<EventListenerSupplier<? extends Event>> s = suppliers.computeIfAbsent(event.getClass(), key -> {
            final Class<? extends Event> impl = getImplementingEvent(event.getEventType());
            LOGGER.debug("Event {} implements {}.", event.getClass(), impl);
            return new ArrayList<>(ListenerServiceLoader.load(impl));
        });
        // send the event to all listeners
        s.stream().map(Supplier::get)
                .flatMap(l -> l.map(Stream::of).orElse(Stream.empty()))
                .forEach(l -> fire(event, (EventListener<Event>) l));
    }

    private static class MyEventFiringListener implements EventFiringListener {

        private final SessionInfo sessionInfo;

        public MyEventFiringListener(final SessionInfo sessionInfo) {
            this.sessionInfo = sessionInfo;
        }

        @Override
        public void requested(final LazyEvent<? extends Event> event) {
            LOGGER.debug("Requested firing {} for {}.", event.getEventType(), sessionInfo);
        }

        @Override
        public void queued(final Event event) {
            LOGGER.debug("Queued firing {} for {}.", event, sessionInfo);
        }

        @Override
        public void fired(final Event event) {
            LOGGER.debug("Fired {} for {}.", event, sessionInfo);
        }

        @Override
        public void failed(final Event event, final Exception ex) {
            LOGGER.warn("Listener failed for {}.", event, ex);
        }
    }
}
