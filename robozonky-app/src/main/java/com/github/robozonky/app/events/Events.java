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

import java.util.concurrent.CompletableFuture;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;

public interface Events {

    static Events allSessions() {
        return AllSessionEvents.get();
    }

    static SessionEvents forSession(final SessionInfo sessionInfo) {
        return SessionEventsImpl.forSession(sessionInfo);
    }

    /**
     * Transforms given {@link Event} into {@link LazyEvent} and delegates to {@link #fire(LazyEvent)}.
     * @param event
     * @return
     */
    @SuppressWarnings("unchecked")
    default CompletableFuture<Void> fire(final Event event) {
        return fire(new LazyEventImpl<>((Class<Event>) event.getClass(), () -> event));
    }

    /**
     * Send the {@link Event} to all the {@link EventListener}s registered for it. May not instantiate the event in case
     * there are no registered {@link EventListener}s. May hand the notifications to a background thread. Will catch all
     * exceptions and log them.
     * @param event
     * @return When done, the event is guaranteed to be processed by all registered listener.
     */
    CompletableFuture<Void> fire(LazyEvent<? extends Event> event);
}
