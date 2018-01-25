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

package com.github.robozonky.api.marketplaces;

import java.util.Collection;
import java.util.function.Consumer;

import com.github.robozonky.api.remote.entities.Loan;

/**
 * Represents code that checks some remote marketplace for new loans. Use {@link #registerListener(Consumer)} as an
 * entry point. It is expected that implementations will be executed off the main thread. Implementations may only
 * become active when the {@link #run()} method has been called, and will cease all operations when {@link #close()}
 * is called.
 */
public interface Marketplace extends AutoCloseable,
                                     Runnable {

    /**
     * Tell the marketplace to send all new loans to a particular endpoint. The runtime promises to execute this
     * method before any calls to {@link #run()} are made and never after that.
     * @param listener The endpoint to send loans to.
     * @return True if the listener has been set up to receive new loans, false if already registered before.
     */
    boolean registerListener(Consumer<Collection<Loan>> listener);

    /**
     * This method is supposed to check the marketplace once and terminate.
     */
    @Override
    void run();

    @Override
    default void close() {
        // don't force implementations to override
    }
}
