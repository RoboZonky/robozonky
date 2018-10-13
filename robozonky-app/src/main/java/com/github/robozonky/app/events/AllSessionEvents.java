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
import com.github.robozonky.internal.util.LazyInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class AllSessionEvents implements Events {

    private static final Logger LOGGER = LoggerFactory.getLogger(AllSessionEvents.class);
    private static final LazyInitialized<AllSessionEvents> INSTANCE = LazyInitialized.create(AllSessionEvents::new);

    private AllSessionEvents() {
        // no external instances
    }

    static Events get() {
        return INSTANCE.get();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public CompletableFuture<Void> fire(final LazyEvent<? extends Event> event) {
        LOGGER.debug("Firing {} for all sessions.", event);
        final CompletableFuture[] futures = SessionEventsImpl.all().stream()
                .map(s -> s.fire(event))
                .toArray(CompletableFuture[]::new);
        return CompletableFuture.allOf(futures);
    }
}
