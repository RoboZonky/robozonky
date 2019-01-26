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

package com.github.robozonky.notifications;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.common.async.Refreshable;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class NotificationEventListenerSupplier<T extends Event> implements Refreshable.RefreshListener<ConfigStorage>,
                                                                          Function<Target, Optional<EventListener<T>>> {

    private static final Logger LOGGER = LogManager.getLogger(NotificationEventListenerSupplier.class);

    private final Class<T> eventType;
    private final AtomicReference<Map<Target, EventListener<T>>> value = new AtomicReference<>(Collections.emptyMap());

    public NotificationEventListenerSupplier(final Class<T> eventType) {
        this.eventType = eventType;
    }

    private static AbstractTargetHandler getTargetHandler(final ConfigStorage configStorage, final Target target) {
        switch (target) {
            case EMAIL:
                return new EmailHandler(configStorage);
            default:
                throw new IllegalArgumentException("Unsupported target: " + target);
        }
    }

    public Optional<EventListener<T>> apply(final Target target) {
        return Optional.ofNullable(value.get().get(target));
    }

    @Override
    public void valueSet(final ConfigStorage newValue) {
        final Map<Target, EventListener<T>> result = new EnumMap<>(Target.class);
        for (final Target target : Target.values()) {
            final AbstractTargetHandler handler = getTargetHandler(newValue, target);
            if (!handler.isEnabledInSettings()) {
                LOGGER.debug("Notifications are disabled: {}.", target.getId());
                continue;
            }
            final Optional<EventListener<T>> maybe = findListener(handler);
            maybe.ifPresent(listener -> result.put(target, listener));
        }
        value.set(result);
    }

    @SuppressWarnings("unchecked")
    private Optional<EventListener<T>> findListener(final AbstractTargetHandler handler) {
        final EventListener<T> result = Stream.of(SupportedListener.values())
                .filter(l -> Objects.equals(eventType, l.getEventType()))
                .peek(l -> LOGGER.trace("Found listener: {}.", l))
                .filter(handler::isEnabled)
                .peek(l -> LOGGER.debug("{} notification enabled for '{}'.", l, handler.getTarget()))
                .findFirst()
                .map(l -> (EventListener<T>) l.getListener(handler))
                .orElse(null);
        return Optional.ofNullable(result);
    }

    @Override
    public void valueUnset(final ConfigStorage oldValue) {
        value.set(Collections.emptyMap());
    }

    @Override
    public void valueChanged(final ConfigStorage oldValue, final ConfigStorage newValue) {
        valueSet(newValue);
    }

    @Override
    public String toString() {
        return "EventListenerSupplier{" +
                "eventType=" + eventType +
                '}';
    }
}
