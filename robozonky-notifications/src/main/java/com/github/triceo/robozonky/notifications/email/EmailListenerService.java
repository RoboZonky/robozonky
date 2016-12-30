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

package com.github.triceo.robozonky.notifications.email;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class EmailListenerService implements ListenerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(EmailListenerService.class);

    private static NotificationProperties getProperties() {
        final Optional<NotificationProperties> optionalProps = NotificationProperties.getProperties();
        if (!optionalProps.isPresent()) {
            EmailListenerService.LOGGER.info("No configuration file found, e-mail notifications disabled.");
            return null;
        }
        final NotificationProperties props = optionalProps.get();
        if (!props.isEnabled()) {
            EmailListenerService.LOGGER.info("E-mail notifications disabled through configuration.");
            return null;
        }
        return props;
    }

    private final NotificationProperties properties = EmailListenerService.getProperties();

    @Override
    public <T extends Event> Optional<EventListener<T>> findListener(final Class<T> eventType) {
        if (this.properties == null) {
            return Optional.empty();
        }
        return Stream.of(SupportedListener.values())
                .filter(l -> Objects.equals(eventType, l.getEventType()))
                .filter(properties::isListenerEnabled)
                .findFirst()
                .map(l -> Optional.of((EventListener<T>)l.getListener(this.properties)))
                .orElse(Optional.empty());
    }
}
