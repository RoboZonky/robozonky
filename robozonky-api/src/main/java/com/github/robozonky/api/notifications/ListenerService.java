/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.api.notifications;

import java.util.ServiceLoader;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;

/**
 * Use Java's {@link ServiceLoader} to load {@link EventListener}s
 */
@FunctionalInterface
public interface ListenerService {

    /**
     * Load event listeners that the service wants to register. These listeners will be called during regular
     * operations and as such must never block for a prolonged period of time. Do not expect these listeners to be
     * called in any specific order or on any specific thread.
     * 
     * @param sessionInfo The tenant for which to retrieve the listeners.
     * @param eventType   Type of the event listener to find.
     * @param <T>         Class describing the event.
     * @return A listener, if any, to register with RoboZonky. Null means event type is not supported at all.
     */
    <T extends Event> Stream<EventListenerSupplier<T>> findListeners(final SessionInfo sessionInfo,
            final Class<T> eventType);
}
