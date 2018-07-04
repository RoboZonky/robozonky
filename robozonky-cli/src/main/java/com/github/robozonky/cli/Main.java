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

package com.github.robozonky.cli;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(final String... args) {
        final Optional<Feature> featureOptional = CommandLine.parse(args);
        if (featureOptional.isPresent()) {
            final Feature feature = featureOptional.get();
            try {
                LOGGER.info("Will execute: '{}'", feature.describe());
                feature.setup();
                LOGGER.info("Executed, running test.");
                feature.test();
                LOGGER.info("Success.");
            } catch (final SetupFailedException e) {
                LOGGER.warn("Could not perform setup.", e);
                System.exit(2);
            } catch (final TestFailedException e) {
                LOGGER.warn("Could not test setup.", e);
                System.exit(3);
            }
        } else { // call the thing again, requesting help message
            CommandLine.parse("-h");
            System.exit(1);
        }
    }
}
