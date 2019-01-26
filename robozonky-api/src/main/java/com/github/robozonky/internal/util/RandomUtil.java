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

package com.github.robozonky.internal.util;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * All RoboZonky code should use this class to retrieve its random numbers. This will ensure that tests will be able to
 * inject their own {@link Random} implementation.
 * <p>
 * By default, this uses a {@link ThreadLocalRandom} on account of RoboZonky being a concurrent application.
 */
public final class RandomUtil extends Random {

    private static final Logger LOGGER = LogManager.getLogger(RandomUtil.class);
    private static final Supplier<Random> DEFAULT = ThreadLocalRandom::current;
    private static final AtomicReference<Supplier<Random>> RANDOM = new AtomicReference<>(DEFAULT);

    private RandomUtil() {
        // no instances
    }

    private static Random getRandom() {
        return RANDOM.get().get();
    }

    static void setRandom(final Random random) {
        RANDOM.set(() -> random);
        LOGGER.debug("Set a custom random generator: {}.", random);
    }

    static void resetRandom() {
        RANDOM.set(DEFAULT);
        LOGGER.debug("Reset to original random generator.");
    }

    /**
     * Will call {@link Random#nextInt()} on the {@link Random} implementation currently used.
     * @return Return of the call.
     */
    public static int getNextInt() {
        return getRandom().nextInt();
    }

    /**
     * Will call {@link Random#nextInt(int)} on the {@link Random} implementation currently used.
     * @return Return of the call.
     */
    public static int getNextInt(final int bound) {
        return getRandom().nextInt(bound);
    }
}
