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

package com.github.robozonky.test;

import java.util.Properties;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

enum SystemProperties {

    INSTANCE;

    private final Logger logger = LogManager.getLogger(SystemProperties.class);
    private Properties originalProperties;

    public void save() {
        originalProperties = System.getProperties();
        System.setProperties(copyOf(originalProperties));
        logger.debug("Storing properties: {}.", originalProperties);
    }

    private static Properties copyOf(final Properties source) {
        final Properties copy = new Properties();
        copy.putAll(source);
        return copy;
    }

    public void restore() {
        logger.debug("Overwriting original properties: {}.", System.getProperties());
        System.setProperties(originalProperties);
    }
}
