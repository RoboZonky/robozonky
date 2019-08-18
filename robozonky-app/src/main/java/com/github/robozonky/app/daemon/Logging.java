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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This class exists so that various parts of the daemon can log through the same {@link Logger} and therefore all
 * similar operations can be easily queried. To facilitate this, all investing code must log through
 * {@link #investing()} etc.
 */
final class Logging {

    private Logging() {
        // no external instances
    }

    public static Logger investing() {
        return Investing.LOGGER;
    }

    public static Logger purchasing() {
        return Purchasing.LOGGER;
    }

    public static Logger reservations() {
        return Reservations.LOGGER;
    }

    public static Logger selling() {
        return Selling.LOGGER;
    }

    private static final class Investing {

        private static final Logger LOGGER = LogManager.getLogger(Investing.class);
    }

    private static final class Purchasing {

        private static final Logger LOGGER = LogManager.getLogger(Purchasing.class);
    }

    private static final class Reservations {

        private static final Logger LOGGER = LogManager.getLogger(Reservations.class);
    }

    private static final class Selling {

        private static final Logger LOGGER = LogManager.getLogger(Selling.class);
    }
}
