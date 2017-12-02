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
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;

class RefreshableEventListener<T extends Event> extends Refreshable<EventListener<T>> {

    private final Supplier<Optional<NotificationProperties>> properties;
    private final Class<T> eventType;

    public RefreshableEventListener(final Supplier<Optional<NotificationProperties>> properties,
                                    final Class<T> eventType) {
        this.properties = properties;
        this.eventType = eventType;
    }

    @Override
    protected Optional<String> getLatestSource() {
        return properties.get().map(Object::toString);
    }

    @Override
    protected Optional<EventListener<T>> transform(final String source) {
        final Optional<NotificationProperties> optionalProps = properties.get();
        return optionalProps.flatMap(props -> {
            if (!props.isEnabled()) {
                LOGGER.debug("E-mail notifications disabled in settings.");
                return Optional.empty();
            }
            return Stream.of(SupportedListener.values())
                    .filter(l -> Objects.equals(eventType, l.getEventType()))
                    .peek(l -> LOGGER.trace("Found listener: {}.", l))
                    .filter(props::isListenerEnabled)
                    .peek(l -> LOGGER.trace("Will call listener: {}.", l))
                    .findFirst().map(l -> (EventListener<T>) l.getListener(props));
        });
    }

    @Override
    public String toString() {
        return "RefreshableEventListener{eventType=" + eventType.getName() + "}";
    }
}
