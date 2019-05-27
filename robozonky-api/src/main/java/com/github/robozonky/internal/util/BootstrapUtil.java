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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class BootstrapUtil {

    private BootstrapUtil() {
        // no instances
    }

    /**
     * Transfers java.util.logging to Log4j. Make sure no {@link Logger} is instantiated before this method is called.
     */
    public static void configureLogging() {
        System.getProperty("java.util.logging.manager", "org.apache.logging.log4j.jul.LogManager");
        LogManager.getLogger(BootstrapUtil.class).debug("Attempted to forward java.util.logging to Log4j.");
    }
}
