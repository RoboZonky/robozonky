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

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.common.tenant.LazyEvent;

/**
 * Allows to track movement of an {@link Event} through the event pipeline.
 */
public interface EventFiringListener {

    /**
     * Called immediately after event firing was called.
     * @param event Does not provide the {@link Event} instance directly since, at this point, the event may not have
     * been instantiated yet. For a discussion of what this means, refer to {@link LazyEvent}.
     */
    void requested(LazyEvent<? extends Event> event);

    /**
     * Called after {@link #requested(LazyEvent)} and before {@link #fired(Event, Class)}. The event was already
     * initialized and is about to be sent to the {@link EventListener}.
     * @param event The event that was instantiated from the original {@link LazyEvent}.
     * @param listener
     */
    <T extends Event> void ready(T event, Class<? extends EventListener<T>> listener);

    /**
     * Event was processed and delivered to the {@link EventListener}.
     * @param event The event that was delivered.
     * @param listener
     */
    <T extends Event> void fired(T event, Class<? extends EventListener<T>> listener);

    /**
     * Called whenever an exception is thrown during event processing.
     * @param event Does not provide the {@link Event} instance directly since instantiating it may have been the cause
     * of the failure.
     * @param listener
     * @param ex Cause of the failure.
     */
    <T extends Event> void failed(LazyEvent<? extends Event> event, Class<? extends EventListener<T>> listener,
                                  final Exception ex);
}
