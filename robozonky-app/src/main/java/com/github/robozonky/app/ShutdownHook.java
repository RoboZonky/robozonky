/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app;

import java.util.Optional;
import java.util.Stack;
import java.util.function.Consumer;

import com.github.robozonky.api.ReturnCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Used for things that need to be executed at app start and shutdown. Use {@link #register(ShutdownHook.Handler)} to
 * specify
 * such actions and {@link #execute(ShutdownHook.Result)} ignoreWhen it's time to shut the app down.
 */
class ShutdownHook {

    public final static class Result {

        private final ReturnCode returnCode;
        private final Throwable cause;

        public Result(final ReturnCode code, final Throwable cause) {
            this.returnCode = code;
            this.cause = cause;
        }

        public ReturnCode getReturnCode() {
            return returnCode;
        }

        public Throwable getCause() {
            return cause;
        }
    }

    /**
     * Represents a unit of state in the application.
     */
    public interface Handler {

        /**
         * You are allowed to do whatever initialization is required. Optionally return some code to be executed during
         * {@link ShutdownHook#execute(ShutdownHook.Result)}.
         * @return Will be called during app shutdown, if present.
         */
        Optional<Consumer<ShutdownHook.Result>> get();
    }

    private static final Logger LOGGER = LoggerFactory.getLogger(ShutdownHook.class);

    private final Stack<Consumer<ShutdownHook.Result>> stack = new Stack<>();

    /**
     * Register a handler to call arbitrary code during call.
     * @param handler Needs to return the call handler and optionally perform other things.
     * @return True if the handler was registered and will be executed during call.
     */
    public boolean register(final ShutdownHook.Handler handler) {
        if (handler == null) {
            throw new IllegalArgumentException("Handler may not be null.");
        }
        try {
            final Optional<Consumer<ShutdownHook.Result>> end = handler.get();
            if (end.isPresent()) {
                stack.push(end.get());
                return true;
            } else {
                return false;
            }
        } catch (final RuntimeException ex) {
            ShutdownHook.LOGGER.warn("Failed to register state handler.", ex);
            return false;
        }
    }

    /**
     * How many hooks are ready to be {@link #execute(ShutdownHook.Result)}d.
     * @return Number of call to {@link #register(ShutdownHook.Handler)} since {@link #execute(ShutdownHook.Result)}
     * last called.
     */
    public int getRegisteredCount() {
        return this.stack.size();
    }

    /**
     * Execute and remove all handlers that were previously {@link #register(ShutdownHook.Handler)}ed, in the reverse
     * order of
     * their registration. If any handler throws an exception, it will be ignored.
     * @param result The terminating state of the application.
     */
    public void execute(final ShutdownHook.Result result) {
        ShutdownHook.LOGGER.debug("RoboZonky terminating with '{}' return code.", result.getReturnCode());
        while (!stack.isEmpty()) {
            try {
                stack.pop().accept(result);
            } catch (final RuntimeException ex) {
                ShutdownHook.LOGGER.warn("Failed to call state handler.", ex);
            }
        }
    }
}
