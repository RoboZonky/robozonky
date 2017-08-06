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

package com.github.triceo.robozonky.notifications.email;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;

class RefreshableEventListener<T extends Event> extends Refreshable<EventListener<T>> {

    private final RefreshableNotificationProperties properties;
    private final Class<T> eventType;

    public RefreshableEventListener(final RefreshableNotificationProperties properties, final Class<T> eventType) {
        this.properties = properties;
        this.eventType = eventType;
    }

    @Override
    protected Supplier<Optional<String>> getLatestSource() {
        properties.run();
        return () -> properties.getLatest().map(Object::toString);
    }

    @Override
    protected Optional<EventListener<T>> transform(final String source) {
        final Optional<NotificationProperties> optionalProps = properties.getLatest();
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
