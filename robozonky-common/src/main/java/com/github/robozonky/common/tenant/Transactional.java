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

package com.github.robozonky.common.tenant;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Updates to state, which happen through {@link Tenant}, are postponed until {@link #run()} is called.
 * <p>
 * This class is thread-safe, since multiple threads may want to fire events and/or store state data at the same time.
 */
class Transactional implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Transactional.class);

    private final Queue<Runnable> stateUpdates = new ConcurrentLinkedQueue<>();

    Queue<Runnable> getStateUpdates() {
        return stateUpdates;
    }

    /**
     * Fire events and update state. Clears internal state, so that the next {@link #run()} call would not do anything
     * unless state updates are performed inbetween.
     */
    @SuppressWarnings("unchecked")
    @Override
    public void run() {
        LOGGER.debug("Replaying transaction.");
        while (!stateUpdates.isEmpty()) {
            stateUpdates.poll().run();
        }
    }
}
