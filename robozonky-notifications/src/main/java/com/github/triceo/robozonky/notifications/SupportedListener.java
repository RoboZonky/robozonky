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

package com.github.triceo.robozonky.notifications;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import org.apache.commons.lang3.StringUtils;

public interface SupportedListener<T extends NotificationProperties> {

    /**
     * Return ID of the listener. If listeners have the same ID, it means they share one namespace in configuration.
     * @return ID of the listener which will be used as namespace in the config file.
     */
    default String getLabel() {
        final String className = this.getEventType().getSimpleName();
        final String decapitalized = StringUtils.uncapitalize(className);
        // this works because Event subclasses must be named (Something)Event; check Event().
        return decapitalized.substring(0, decapitalized.length() - "Event".length());
    }

    /**
     * Type of event that this listener responds to.
     * @return Event type.
     */
    Class<? extends Event> getEventType();

    EventListener<? extends Event> getListener(final T properties);
}
