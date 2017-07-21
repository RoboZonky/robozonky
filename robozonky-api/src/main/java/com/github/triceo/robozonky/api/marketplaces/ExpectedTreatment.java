/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.marketplaces;

import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;

/**
 * Specifies supported regimes of treatment of the {@link Marketplace} by the runtime.
 */
public enum ExpectedTreatment {

    /**
     * {@link Marketplace#run()} will be executed on a background thread, {@link Marketplace#close()} will only be
     * called after the user decides to terminate the application. Useful for receiving push notifications from other
     * applications, such as Pushbullet.
     */
    LISTENING,
    /**
     * {@link Marketplace#run()} may be submitted to {@link ScheduledExecutorService} for regular execution, each
     * execution sending new data to registered listener. (See {@link Marketplace#registerListener(Consumer)}.)
     * In this case, {@link Marketplace#close()}  will be called after the user decides to terminate the application.
     * Useful for periodical checks of some remote marketplace, such as Zotify.
     * <p>
     * Please note that this treatment may also result in the {@link Marketplace#run} method being only run once,
     * followed immediately by {@link Marketplace#close()}. The implementor should not expect that this method will
     * be called any more than once.
     */
    POLLING

}
