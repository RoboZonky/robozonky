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

import com.github.triceo.robozonky.api.ReturnCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for things that need to be executed at app start and shutdown. Use {@link #register(State.Handler)} to specify
 * such actions and {@link #shutdown(ReturnCode)} when it's time to shut the app down.
 */
class State {

    /**
     * Represents a unit of state in the application.
     */
    public interface Handler {

        /**
         * You are allowed to do whatever initialization is required. Optionally return some code to be executed during
         * {@link State#shutdown(ReturnCode)}.
         *
         * @return Will be called during app shutdown, if present.
         */
        Optional<Consumer<ReturnCode>> get();

    }

    private static final Logger LOGGER = LoggerFactory.getLogger(State.class);

    private final Stack<Consumer<ReturnCode>> stack = new Stack<>();

    /**
     * Register a handler to call arbitrary code during shutdown.
     *
     * @param handler Needs to return the shutdown handler and optionally perform other things.
     * @return True if the handler was registered and will be executed during shutdown.
     */
    public boolean register(final State.Handler handler) {
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
     * Execute and remove all handlers that were previously {@link #register(State.Handler)}ed, in the reverse order of
     * their registration. If any handler throws an exception, it will be ignored.
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
    }

}
