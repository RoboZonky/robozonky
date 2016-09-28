/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Handler for things that need to be executed at shutdown. Use {@link #register(Supplier)} to specify such actions and
 * {@link #shutdown(ReturnCode)} when it's time to shut down.
 */
class State {

    private static final Logger LOGGER = LoggerFactory.getLogger(State.class);

    private final Stack<Consumer<ReturnCode>> stack = new Stack<>();

    /**
     * Register a handler to call arbitrary code during shutdown.
     *
     * @param handler Needs to return the shutdown handler and optionally perform other things.
     * @return True if the handler was registered and will be executed during shutdown.
     */
    public boolean register(final Supplier<Optional<Consumer<ReturnCode>>> handler) {
        try {
            final Optional<Consumer<ReturnCode>> end = handler.get();
            if (end.isPresent()) {
                stack.push(end.get());
                return true;
            } else {
                return false;
            }
        } catch (final RuntimeException ex) {
            State.LOGGER.warn("Failed to register state handler.", ex);
            return false;
        }
    }

    /**
     * Execute and remove all handlers that were previously {@link #register(Supplier)}ed, in the reverse order of their
     * registration. If any handler throws an exception, it will be skipped.
     *
     * @param returnCode The application's return code to pass to the handlers.
     */
    public void shutdown(final ReturnCode returnCode) {
        State.LOGGER.debug("RoboZonky terminating with '{}' return code.", returnCode);
        while (!stack.isEmpty()) {
            try {
                stack.pop().accept(returnCode);
            } catch (final RuntimeException ex) {
                State.LOGGER.warn("Failed to execute state handler.", ex);
            }
        }
        State.LOGGER.info("===== RoboZonky out. =====");
        System.exit(returnCode.getCode());
    }

}
