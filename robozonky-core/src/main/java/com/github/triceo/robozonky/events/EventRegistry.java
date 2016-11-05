/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.events;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for registering listeners and distributing events to those listeners. There are two types of listeners .
 * Local listeners only listen for events of a specific type and are registered through
 * {@link #addListener(Class, EventListener)}. Global listeners listen for all types of events and are registered
 * through {@link #addListener(EventListener)}.
 */
public enum EventRegistry {

    /**
     * Simple cheap thread-safe singleton.
     */
    INSTANCE;

    private static final Logger LOGGER = LoggerFactory.getLogger(EventRegistry.class);

    private static class EventSpecific<E extends Event> {

        private final Set<EventListener<E>> listeners = new CopyOnWriteArraySet<>();

        public boolean addListener(final EventListener<E> eventListener) {
            return listeners.add(eventListener);
        }

        public boolean removeListener(final EventListener<E> eventListener) {
            return listeners.remove(eventListener);
        }

        public Collection<EventListener<E>> getListeners() {
            return Collections.unmodifiableSet(new LinkedHashSet<>(this.listeners));
        }

    }

    private static <E extends Event> void fire(final E event, final EventListener<E> listener) {
        try {
            listener.handle(event);
        } catch (final RuntimeException ex) {
            EventRegistry.LOGGER.warn("Listener failed: {}.", listener, ex);
        }
    }

    /**
     * Distribute a particular event to all listeners that have been added and not yet removed for that particular event
     * and to all such global listeners. This MUST NOT be called by users and is not part of the public API.
     * @param event Event to distribute.
     * @param <E> Event type to distribute. Ignored for global listeners.
     */
    @SuppressWarnings("unchecked")
    public static <E extends Event> void fire(final E event) {
        EventRegistry.LOGGER.debug("Firing {}.", event);
        // fire global listeners
        final Set<EventListener<Event>> globals;
        synchronized (EventRegistry.INSTANCE) {
            globals = new LinkedHashSet<>(EventRegistry.INSTANCE.globalListeners);
        }
        globals.forEach(l -> EventRegistry.fire(event, l));
        // fire event-specific listeners
        final EventRegistry.EventSpecific<E> registry;
        synchronized (EventRegistry.INSTANCE) {
            registry = (EventRegistry.EventSpecific<E>) EventRegistry.INSTANCE.registries.get(event.getClass());
        }
        if (registry != null) {
            registry.getListeners().forEach(l -> EventRegistry.fire(event, l));
        }
    }

    private final Map<Class<? extends Event>, EventRegistry.EventSpecific<? extends Event>> registries =
            new LinkedHashMap<>();
    private final Set<EventListener<Event>> globalListeners = new LinkedHashSet<>();

    /**
     * Registers a global listener that will listen for all kinds of events.
     * @param listener Listener to register.
     * @return True if added, false if already registered.
     */
    public synchronized boolean addListener(final EventListener<Event> listener) {
        return globalListeners.add(listener);
    }

    /**
     * Removes a listener previously registered through {@link #addListener(EventListener)}.
     * @param listener Listener to de-register.
     * @return True if removed, false if never registered.
     */
    public synchronized boolean removeListener(final EventListener<Event> listener) {
        return globalListeners.remove(listener);
    }

    /**
     * Registers a local listener that will listen for a particular type of event.
     * @param eventType Type of event to listen to.
     * @param eventListener Listener to register.
     * @param <E> Generic type of event to listen to.
     * @return True if added, false if already registered.
     */
    @SuppressWarnings("unchecked")
    public synchronized <E extends Event> boolean addListener(final Class<E> eventType, final EventListener<E>
            eventListener) {
        if (!registries.containsKey(eventType)) {
            registries.put(eventType, new EventRegistry.EventSpecific<>());
        }
        final EventRegistry.EventSpecific<E> registry = (EventRegistry.EventSpecific<E>) registries.get(eventType);
        return registry.addListener(eventListener);
    }

    /**
     * Removes a listener previously registered through {@link #removeListener(Class, EventListener)}.
     * @param eventType Type of event to listen to.
     * @param eventListener Listener to register.
     * @param <E> Generic type of event to listen to.
     * @return True if removed, false if never registered.
     */
    @SuppressWarnings("unchecked")
    public synchronized <E extends Event> boolean removeListener(final Class<E> eventType, final EventListener<E>
            eventListener) {
        if (registries.containsKey(eventType)) {
            final EventRegistry.EventSpecific<E> registry = (EventRegistry.EventSpecific<E>) registries.get(eventType);
            return registry.removeListener(eventListener);
        } else {
            return false;
        }
    }

    /**
     * Reset the registry to the original state, no listeners present. Mostly for testing purposes.
     */
    synchronized void clear() {
        this.globalListeners.clear();
        this.registries.clear();
    }

}
