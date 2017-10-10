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

package com.github.robozonky.app.configuration.daemon;

import java.util.function.Consumer;

import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.util.DaemonRuntimeExceptionHandler;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class DaemonOperation implements Runnable {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private final Authenticated api;
    private final Consumer<Zonky> investor;

    protected DaemonOperation(final Authenticated auth, final Consumer<Zonky> operation) {
        this.api = auth;
        this.investor = operation;
    }

    @Override
    public void run() {
        try {
            LOGGER.trace("Starting.");
            api.run(investor);
            LOGGER.trace("Finished.");
        } catch (final Throwable t) {
            // we catch Throwable so that we can inform users even about errors
            new DaemonRuntimeExceptionHandler().handle(t);
        }
    }
}
