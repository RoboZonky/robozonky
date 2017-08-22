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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.api.notifications.SessionInfo;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for registering distributing events to listener registered through {@link ListenerService}.
 * <p>
 * No guarantees are given as to the order in which the listeners will be executed.
 */
public enum Events {

    /**
     * Simple cheap thread-safe singleton.
     */
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(Events.class);
    private static final List<Event> EVENTS_FIRED = new ArrayList<>();
    private static SessionInfo SESSION_INFO = null;

    private static class EventSpecific<E extends Event> {

        private final Set<Refreshable<EventListener<E>>> listeners = new LinkedHashSet<>();

        public void addListener(final Refreshable<EventListener<E>> eventListener) {
            listeners.add(eventListener);
        }

        public Stream<Refreshable<EventListener<E>>> getListeners() {
            return this.listeners.stream();
        }
    }

    private static <E extends Event> void fire(final E event, final EventListener<E> listener, final SessionInfo info) {
        try {
            listener.handle(event, info);
        } catch (final RuntimeException ex) {
            Events.LOGGER.warn("Listener failed: {}.", listener, ex);
        }
    }

    final Map<Class<? extends Event>, Events.EventSpecific<? extends Event>> registries = new HashMap<>();

    /**
     * Retrieve all listeners registered for a given event type. During the first call of this method, it will use the
     * {@link ListenerService} to register all available listeners. Subsequent calls will only retrieve this
     * information, without querying the service again.
     * @param eventClass Class of event for which to look up listeners.
     * @param <E> Ditto.
     * @return Listeners available for the event type in question.
     */
    @SuppressWarnings("unchecked")
    private synchronized <E extends Event> Events.EventSpecific<E> loadListeners(final Class<E> eventClass) {
        return (Events.EventSpecific<E>) this.registries.computeIfAbsent(eventClass, key -> {
            Events.LOGGER.trace("Registering event listeners for {}.", key);
            final Events.EventSpecific<E> eventSpecific = new Events.EventSpecific<>();
            ListenerServiceLoader.load(eventClass).forEach(eventSpecific::addListener);
            return eventSpecific;
        });
    }

    /**
     * Exists purely for testing purposes. Will call {@link #loadListeners(Class)}, but also add one extra listener.
     * @param eventClass Will be used as argument to the {@link #loadListeners(Class)} call.
     * @param listener The additional listener to register.
     * @param <E> Type of event to register listeners for.
     * @return Registered listeners.
     */
    @SuppressWarnings("unchecked")
    <E extends Event> Events.EventSpecific<E> loadListeners(final Class<E> eventClass,
                                                            final Refreshable<EventListener<E>> listener) {
        final Events.EventSpecific<E> eventSpecific = loadListeners(eventClass);
        if (listener != null) {
            eventSpecific.addListener(listener);
        }
        return eventSpecific;
    }

    @SuppressWarnings("unchecked")
    <E extends Event> Stream<EventListener<E>> getListeners(final Class<E> eventClass) {
        return this.loadListeners(eventClass).getListeners()
                .flatMap(r -> r.getLatest().map(Stream::of).orElse(Stream.empty()));
    }

    /**
     * Distribute a particular event to all listeners that have been added and not yet removed for that particular
     * event. This MUST NOT be called by users and is not part of the public API.
     * <p>
     * The listeners may be executed in parallel, no execution order guarantees are given. When this method returns,
     * all listeners' {@link EventListener#handle(Event, SessionInfo)} method will have returned. Will use the
     * internal {@link SessionInfo} instance for that.
     * @param event Event to distribute.
     * @param sessionInfo If not null, internal {@link SessionInfo} instance will be updated.
     * @param <E> Event type to distribute.
     */
    public synchronized static <E extends Event> void fire(final E event, final SessionInfo sessionInfo) {
        if (sessionInfo != null) {
            Events.SESSION_INFO = sessionInfo;
        }
        final Class<E> eventClass = (Class<E>) event.getClass();
        Events.LOGGER.debug("Firing {}.", eventClass);
        Events.INSTANCE.getListeners(eventClass).parallel().forEach(l -> Events.fire(event, l, Events.SESSION_INFO));
        Events.LOGGER.trace("Fired {}.", event);
        if (Settings.INSTANCE.isDebugEventStorageEnabled()) {
            Events.EVENTS_FIRED.add(event);
        }
    }

    /**
     * Distribute a particular event to all listeners that have been added and not yet removed for that particular
     * event. This MUST NOT be called by users and is not part of the public API.
     * <p>
     * The listeners may be executed in parallel, no execution order guarantees are given. When this method returns,
     * all listeners' {@link EventListener#handle(Event, SessionInfo)} method will have returned.
     * <p>
     * This method will not update the internal {@link SessionInfo} instance.
     * @param event Event to distribute.
     * @param <E> Event type to distribute.
     */
    @SuppressWarnings("unchecked")
    public static <E extends Event> void fire(final E event) {
        Events.fire(event, null);
    }

    /**
     * This only exists for testing purposes. Also see {@link Settings#isDebugEventStorageEnabled()}.
     * @return Events that were stored, if any. Returns the storage directly, any mutation operations will mutate the
     * storage.
     */
    public static List<Event> getFired() {
        return Events.EVENTS_FIRED;
    }

}
