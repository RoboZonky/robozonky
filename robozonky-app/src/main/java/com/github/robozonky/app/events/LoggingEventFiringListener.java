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

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LoggingEventFiringListener implements EventFiringListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(LoggingEventFiringListener.class);

    private final SessionInfo sessionInfo;

    public LoggingEventFiringListener(final SessionInfo sessionInfo) {
        this.sessionInfo = sessionInfo;
    }

    @Override
    public void requested(final LazyEvent<? extends Event> event) {
        LOGGER.trace("Requested firing {} for {}.", event.getEventType(), sessionInfo);
    }

    @Override
    public <T extends Event> void queued(final T event, final Class<? extends EventListener<T>> listener) {
        LOGGER.trace("Queued {} to {} for {}.", event, listener, sessionInfo);
    }

    @Override
    public <T extends Event> void fired(final T event, final Class<? extends EventListener<T>> listener) {
        LOGGER.debug("Sent {} to {} for {}.", event, listener, sessionInfo);
    }

    @Override
    public <T extends Event> void failed(final LazyEvent<? extends Event> event,
                                         final Class<? extends EventListener<T>> listener, final Exception ex) {
        LOGGER.warn("Listener {} failed for {}.", listener, event, ex);
    }

}
