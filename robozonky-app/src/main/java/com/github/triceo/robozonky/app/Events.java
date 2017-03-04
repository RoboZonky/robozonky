/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ListenerService;
import com.github.triceo.robozonky.common.extensions.ListenerServiceLoader;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.util.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for registering distributing events to listener registered through {@link ListenerService}.
 *
 * No guarantees are given as to the order in which the listeners will be executed.
 */
public enum Events {

    /**
     * Simple cheap thread-safe singleton.
     */
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(Events.class);
    private static final List<Event> EVENTS_FIRED = new ArrayList<>();

    private static class EventSpecific<E extends Event> {

        private final Set<Refreshable<EventListener<E>>> listeners = new HashSet<>();

        public void addListener(final Refreshable<EventListener<E>> eventListener) {
            listeners.add(eventListener);
        }

        public Collection<Refreshable<EventListener<E>>> getListeners() {
            return Collections.unmodifiableSet(new HashSet<>(this.listeners)); // defensive copy
        }

    }

    private static <E extends Event> void fire(final E event, final EventListener<E> listener) {
        try {
            listener.handle(event);
        } catch (final RuntimeException ex) {
            Events.LOGGER.warn("Listener failed: {}.", listener, ex);
        }
    }

    final Map<Class<? extends Event>, Events.EventSpecific<? extends Event>> registries = new HashMap<>();

    @SuppressWarnings("unchecked")
    private <E extends Event> void loadListeners(final Class<E> eventClass) {
        this.loadListeners(eventClass, null);
    }

    @SuppressWarnings("unchecked")
    synchronized <E extends Event> void loadListeners(final Class<E> eventClass,
                                                      final Refreshable<EventListener<E>> listener) {
        if (this.registries.containsKey(eventClass)) {
            return;
        }
        Events.LOGGER.trace("Registering event listeners for {}.", eventClass);
        this.registries.put(eventClass, new Events.EventSpecific<E>());
        final Stream<Refreshable<EventListener<E>>> listeners = Stream.concat(
                ListenerServiceLoader.load(eventClass, Scheduler.BACKGROUND_SCHEDULER).stream(),
                listener == null ? Stream.empty() : Stream.of(listener)
        );
        listeners.forEach(l -> ((Events.EventSpecific<E>) this.registries.get(eventClass)).addListener(l));
    }

    @SuppressWarnings("unchecked")
    synchronized <E extends Event> Stream<Refreshable<EventListener<E>>> getListeners(final Class<E> eventClass) {
        this.loadListeners(eventClass);
        return ((Events.EventSpecific<E>) this.registries.get(eventClass)).getListeners().stream();
    }

    /**
     * Distribute a particular event to all listeners that have been added and not yet removed for that particular
     * event. This MUST NOT be called by users and is not part of the public API.
     * <p>
     * The listeners may be executed in parallel, no execution order guarantees are given. When this method returns,
     * all listeners' {@link EventListener#handle(Event)} method will have returned.
     * @param event Event to distribute.
     * @param <E> Event type to distribute.
     */
    @SuppressWarnings("unchecked")
    public static <E extends Event> void fire(final E event) {
        final Class<E> eventClass = (Class<E>) event.getClass();
        Events.LOGGER.debug("Firing {}.", eventClass);
        Events.INSTANCE.getListeners(eventClass).parallel()
                .flatMap(r -> r.getLatest().map(Stream::of).orElse(Stream.empty()))
                .forEach(l -> Events.fire(event, l));
        if (Defaults.isDebugEventStorageEnabled()) {
            Events.EVENTS_FIRED.add(event);
        }
    }

    /**
     * This only exists for testing purposes. Also see {@link Defaults#isDebugEventStorageEnabled()}.
     *
     * @return Events that were stored, if any. Returns the storage directly, any mutation operations will mutate the
     * storage.
     */
    public static List<Event> getFired() {
        return Events.EVENTS_FIRED;
    }

}
