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

package com.github.robozonky.app.daemon;

import java.util.function.Consumer;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class SimpleSkippable implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(SimpleSkippable.class);

    private final Class<?> type;
    private final Runnable toRun;
    private final Consumer<Throwable> shutdownCall;

    SimpleSkippable(final Runnable toRun, final Class<?> type, final Consumer<Throwable> shutdownCall) {
        this.toRun = toRun;
        this.type = type;
        this.shutdownCall = shutdownCall;
    }

    SimpleSkippable(final Runnable toRun, final Consumer<Throwable> shutdownCall) {
        this(toRun, toRun.getClass(), shutdownCall);
    }

    SimpleSkippable(final Runnable toRun) {
        this(toRun, t -> {
            // do nothing
        });
    }

    @Override
    public void run() {
        LOGGER.trace("Running {}.", this);
        try {
            toRun.run();
            LOGGER.trace("Successfully finished {}.", this);
        } catch (final Exception ex) {
            LOGGER.debug("Failed executing {}.", this, ex);
        } catch (final Error er) {
            shutdownCall.accept(er);
            throw er; // rethrow the error
        }
    }

    @Override
    public String toString() {
        return "SimpleSkippable{" +
                "type=" + type.getCanonicalName() +
                '}';
    }
}
