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
import java.util.stream.Stream;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.notifications.RefreshableEventListener;

class RefreshableEmailEventListener<T extends Event> extends RefreshableEventListener<T, EmailNotificationProperties> {

    public RefreshableEmailEventListener(final Refreshable<EmailNotificationProperties> properties, final Class<T> eventType) {
        super(properties, eventType);
    }

    @Override
    protected Optional<EventListener<T>> transform(final String source) {
        final Optional<EmailNotificationProperties> optionalProps = this.getProperties().getLatest();
        return optionalProps.map(props ->
                Stream.of(SupportedListener.values())
                        .filter(l -> Objects.equals(this.getEventType(), l.getEventType()))
                        .filter(l -> props.isEnabled())
                        .filter(props::isListenerEnabled)
                        .findFirst()
                        .map(l -> Optional.of((EventListener<T>)l.getListener(props)))
                        .orElse(Optional.empty()))
                .orElse(Optional.empty());
    }
}
