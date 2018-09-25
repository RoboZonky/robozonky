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

package com.github.robozonky.app.daemon;

import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Skippable implements Runnable {

    private static final Logger LOGGER = LoggerFactory.getLogger(Skippable.class);

    private final Supplier<Boolean> shouldBeSkipped;
    private final Runnable toRun;

    public Skippable(final Runnable toRun, final Supplier<Boolean> shouldBeSkipped) {
        this.shouldBeSkipped = shouldBeSkipped;
        this.toRun = toRun;
    }

    @Override
    public void run() {
        if (shouldBeSkipped.get()) {
            LOGGER.trace("Not running {}.", toRun);
            return;
        }
        LOGGER.trace("Running {}.", toRun);
        toRun.run();
        LOGGER.trace("Update finished.");
    }

    @Override
    public String toString() {
        return "Skippable{" +
                "toRun=" + toRun +
                '}';
    }
}
