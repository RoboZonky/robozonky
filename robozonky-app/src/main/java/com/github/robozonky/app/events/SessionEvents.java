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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.SessionEvent;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.common.tenant.LazyEvent;
import org.apache.commons.lang3.ClassUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class SessionEvents {

    private static final Logger LOGGER = LogManager.getLogger(SessionEvents.class);
    private static final Map<String, SessionEvents> BY_TENANT = new ConcurrentHashMap<>(0);
    private final Map<Class<?>, List<EventListenerSupplier<? extends Event>>> suppliers = new ConcurrentHashMap<>(0);
    private final Set<EventFiringListener> debugListeners = new CopyOnWriteArraySet<>();
    private final SessionInfo sessionInfo;
    private EventListener<? extends Event> injectedDebugListener;

    private SessionEvents(final SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
        addListener(new LoggingEventFiringListener(sessionInfo));
    }

    static Collection<SessionEvents> all() { // defensive copy
        return Collections.unmodifiableCollection(new ArrayList<>(BY_TENANT.values()));
    }

    static SessionEvents forSession(final SessionInfo sessionInfo) {
        return BY_TENANT.computeIfAbsent(sessionInfo.getUsername(), i -> new SessionEvents(sessionInfo));
    }

    @SuppressWarnings("unchecked")
    static <T extends Event> Class<T> getImplementingEvent(final Class<T> original) {
        final Stream<Class<?>> provided = ClassUtils.getAllInterfaces(original).stream();
        final Stream<Class<?>> interfaces = original.isInterface() ? // interface could be extending it directly
                Stream.concat(Stream.of(original), provided) :
                provided;
        final String apiPackage = "com.github.robozonky.api.notifications";
        return (Class<T>) interfaces.filter(i -> Objects.equals(i.getPackage().getName(), apiPackage))
                .filter(i -> i.getSimpleName().endsWith("Event"))
                .filter(i -> !Objects.equals(i.getSimpleName(), "Event"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not an event:" + original));
    }

    /**
     * Takes a set of {@link Runnable}s and queues them to be fired on a background thread, in the guaranteed order of
     * appearance.
     * @param futures Each item in the stream represents a singular event to be fired.
     * @return When complete, all listeners have been notified of all the events.
     */
    @SuppressWarnings("rawtypes")
    private static Runnable runAsync(final Stream<Runnable> futures) {
        final Runnable[] results = futures.map(EventFiringQueue.INSTANCE::fire)
                .toArray(Runnable[]::new);
        return GlobalEvents.merge(results);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static List<EventListenerSupplier<? extends Event>> retrieveListenerSuppliers(final Class eventType) {
        final Class<? extends Event> impl = getImplementingEvent(eventType);
        LOGGER.trace("Event {} implements {}.", eventType, impl);
        return Collections.unmodifiableList(ListenerServiceLoader.load(impl));
    }

    /**
     * Represents the payload that will be handed over to {@link #runAsync(Stream)}.
     * @param lazyEvent The event which will be instantiated and sent to the listener.
     * @param listener The listener to receive the event.
     * @param <T> Type of the event.
     */
    @SuppressWarnings("unchecked")
    private <T extends Event> void fireAny(final LazyEvent<T> lazyEvent, final EventListener<T> listener) {
        final Class<EventListener<T>> listenerType = (Class<EventListener<T>>) listener.getClass();
        try {
            final T event = lazyEvent.get(); // possibly incurring performance penalties
            debugListeners.forEach(l -> l.ready(event, listenerType));
            listener.handle(event, sessionInfo);
            debugListeners.forEach(l -> l.fired(event, listenerType));
        } catch (final Exception ex) {
            debugListeners.forEach(l -> l.failed(lazyEvent, listenerType, ex));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    Runnable fireAny(final LazyEvent<? extends Event> event) {
        // loan all listeners
        debugListeners.forEach(l -> l.requested(event));
        final Class<? extends Event> eventType = event.getEventType();
        final List<EventListenerSupplier<? extends Event>> s =
                suppliers.computeIfAbsent(eventType, SessionEvents::retrieveListenerSuppliers);
        // send the event to all listeners, execute on the background
        final Stream<EventListener> registered = s.stream()
                .map(Supplier::get)
                .flatMap(l -> l.stream().flatMap(Stream::of));
        final Stream<EventListener> withInjected = injectedDebugListener == null ?
                registered :
                Stream.concat(Stream.of(injectedDebugListener), registered);
        return runAsync(withInjected.map(l -> () -> fireAny(event, l)));
    }

    public boolean addListener(final EventFiringListener listener) {
        LOGGER.debug("Adding listener {} for {}.", listener, sessionInfo);
        return debugListeners.add(listener);
    }

    public boolean removeListener(final EventFiringListener listener) {
        LOGGER.debug("Removing listener {} for {}.", listener, sessionInfo);
        return debugListeners.remove(listener);
    }

    /**
     * Purely for testing purposes.
     * @param listener
     */
    void injectEventListener(final EventListener<? extends Event> listener) {
        this.injectedDebugListener = listener;
    }

    public Runnable fire(final LazyEvent<? extends SessionEvent> event) {
        return fireAny(event);
    }

    @SuppressWarnings("unchecked")
    public Runnable fire(final SessionEvent event) {
        return fire(EventFactory.async((Class<SessionEvent>) event.getClass(), () -> event));
    }
}
