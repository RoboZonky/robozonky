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

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.GlobalEvent;
import com.github.robozonky.app.events.impl.EventFactory;
import io.vavr.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class GlobalEvents {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalEvents.class);
    private static final Lazy<GlobalEvents> INSTANCE = Lazy.of(GlobalEvents::new);

    private GlobalEvents() {
        // no external instances
    }

    static GlobalEvents get() {
        return INSTANCE.get();
    }

    @SuppressWarnings("rawtypes")
    public CompletableFuture<Void> fire(final LazyEvent<? extends GlobalEvent> event) {
        LOGGER.debug("Firing {} for all sessions.", event);
        final CompletableFuture[] futures = SessionEvents.all().stream()
                .map(s -> s.fireAny(event))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }

    /**
     * Transforms given {@link Event} into {@link LazyEvent} and delegates to {@link #fire(LazyEvent)}.
     * @param event
     * @return
     */
    @SuppressWarnings("unchecked")
    public CompletableFuture<Void> fire(final GlobalEvent event) {
        return fire(EventFactory.async((Class<GlobalEvent>) event.getClass(), () -> event));
    }

}
