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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.internal.util.LazyInitialized;
import org.apache.commons.lang3.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Events {

    private static final Logger LOGGER = LoggerFactory.getLogger(Events.class);
    private static final LazyInitialized<Events> INSTANCE = LazyInitialized.create(Events::new);
    private final Map<Class<?>, List<EventListenerSupplier<? extends Event>>> suppliers = new ConcurrentHashMap<>(0);
    private final List<Event> eventsFired = new ArrayList<>(0);
    private SessionInfo sessionInfo = null;

    private Events() {
        // no external instances
    }

    public static Events get() {
        return INSTANCE.get();
    }

    @SuppressWarnings("unchecked")
    static <T extends Event> Class<T> getImplementingEvent(final Class<T> original) {
        return (Class<T>) ClassUtils.getAllInterfaces(original).stream()
                .filter(i -> Objects.equals(i.getPackage().getName(), "com.github.robozonky.api.notifications"))
                .filter(i -> i.getSimpleName().endsWith("Event"))
                .filter(i -> !Objects.equals(i.getSimpleName(), "Event"))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("Not an event:" + original));
    }

    // TODO in a tenant-based world, this is pure wrong. in fact, most of this class is.
    public static void initialize(final SessionInfo info) {
        get().sessionInfo = info;
    }

    @SuppressWarnings("unchecked")
    public void fire(final Event event) {
        LOGGER.debug("Asked to fire {}.", event);
        final List<EventListenerSupplier<? extends Event>> s = suppliers.computeIfAbsent(event.getClass(), key -> {
            final Class<? extends Event> impl = getImplementingEvent(event.getClass());
            LOGGER.debug("Event {} implements {}.", event.getClass(), impl);
            return new ArrayList<>(ListenerServiceLoader.load(impl));
        });
        s.stream().map(Supplier::get)
                .flatMap(l -> l.map(Stream::of).orElse(Stream.empty()))
                .forEach(l -> fire(event, (EventListener<Event>)l, sessionInfo));
        eventsFired.add(event);
    }

    /**
     * This only exists for testing purposes. Also see {@link Settings#isDebugEventStorageEnabled()}.
     * @return Events that were stored, if any. Returns the storage directly, any mutation operations will mutate the
     * storage.
     *
     * TODO in a tenant-based world, this is pure wrong. we need to implement listeners for firing and actual firing.
     */
    public List<Event> getFired() {
        return eventsFired;
    }

    public Optional<SessionInfo> getSessionInfo() {
        return Optional.ofNullable(sessionInfo);
    }

    private static <E extends Event> void fire(final E event, final EventListener<E> listener, final SessionInfo info) {
        try {
            LOGGER.trace("Sending {} to listener {} for {}.", event, listener, info);
            listener.handle(event, info);
        } catch (final RuntimeException ex) {
            LOGGER.warn("Listener failed: {}.", listener, ex);
        } finally {
            LOGGER.trace("Fired.");
        }
    }

}
