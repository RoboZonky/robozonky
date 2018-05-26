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

package com.github.robozonky.notifications.email;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.notifications.configuration.NotificationProperties;
import com.github.robozonky.util.Refreshable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class EmailingEventListenerSupplier<T extends Event> implements Refreshable.RefreshListener<NotificationProperties>,
                                                                EventListenerSupplier<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailingEventListenerSupplier.class);

    private final Class<T> eventType;
    private final AtomicReference<EventListener<T>> value = new AtomicReference<>();

    public EmailingEventListenerSupplier(final Class<T> eventType) {
        this.eventType = eventType;
    }

    @Override
    public Optional<EventListener<T>> get() {
        return Optional.ofNullable(value.get());
    }

    @Override
    public void valueSet(final NotificationProperties newValue) {
        if (!newValue.isEnabled()) {
            LOGGER.debug("E-mail notifications disabled in settings.");
            value.set(null);
        }
        final EventListener<T> result = Stream.of(SupportedListener.values())
                .filter(l -> Objects.equals(eventType, l.getEventType()))
                .peek(l -> LOGGER.trace("Found listener: {}.", l))
                .filter(newValue::isListenerEnabled)
                .peek(l -> LOGGER.trace("Will call listener: {}.", l))
                .findFirst()
                .map(l -> (EventListener<T>) l.getListener(newValue))
                .orElse(null);
        value.set(result);
    }

    @Override
    public void valueUnset(final NotificationProperties oldValue) {
        value.set(null);
    }

    @Override
    public void valueChanged(final NotificationProperties oldValue, final NotificationProperties newValue) {
        valueSet(newValue);
    }

    @Override
    public String toString() {
        return "EventListenerSupplier{" +
                "eventType=" + eventType +
                '}';
    }
}
