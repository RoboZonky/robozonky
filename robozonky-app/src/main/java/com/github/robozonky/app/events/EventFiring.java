/*
 * Copyright 2019 The RoboZonky Project
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

import java.util.concurrent.BlockingQueue;

import com.github.robozonky.common.jobs.SimplePayload;
import jdk.jfr.Event;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Payload for the {@link EventFiringQueue}'s queue-polling thread.
 */
final class EventFiring implements SimplePayload {

    private static final Logger LOGGER = LogManager.getLogger();

    private final BlockingQueue<Runnable> queue;

    public EventFiring() {
        this(EventFiringQueue.INSTANCE.getQueue());
    }

    EventFiring(final BlockingQueue<Runnable> queue) {
        this.queue = queue;
    }

    @Override
    public void run() {
        LOGGER.debug("Starting event firing.");
        boolean repeat = true;
        while (repeat && !queue.isEmpty()) {
            final Event event = new EventFiringJfrEvent();
            try {
                event.begin();
                queue.take().run();
            } catch (final InterruptedException ex) {
                LOGGER.debug("Interrupted while waiting for an event to fire.", ex);
                Thread.currentThread().interrupt();
                repeat = false;
            } finally {
                event.commit();
            }
        }
        LOGGER.debug("Event firing complete.");
    }
}
