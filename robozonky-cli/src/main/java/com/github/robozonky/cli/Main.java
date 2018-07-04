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

import java.io.IOException;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Main {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(final String... args) throws IOException {
        LOGGER.info("Welcome to the RoboZonky command-line configuration and validation tool.");
        LOGGER.warn("This is a tool for the brave. Create a backup copy of RoboZonky or " +
                            "use RoboZonky installer instead.");
        final Optional<Feature> featureOptional = CommandLine.parse(args);
        if (!featureOptional.isPresent()) { // something is wrong, call for help
            CommandLine.parse("-h");
            System.exit(1);
        } else {
            final Feature feature = featureOptional.get();
            if (feature instanceof HelpFeature) {
                // do nothing, help already sent to output
            } else {
                try {
                    LOGGER.info("--- Press any key to run: '{}'", feature.describe());
                    System.in.read();
                    feature.setup();
                    LOGGER.info("--- Executed, running test of the new setup.");
                    feature.test();
                    LOGGER.info("--- Success.");
                } catch (final SetupFailedException e) {
                    LOGGER.warn("!!! Could not perform setup, configuration may have been corrupted.", e);
                    System.exit(2);
                } catch (final TestFailedException e) {
                    LOGGER.warn("!!! Could not test setup, configuration may have been corrupted.", e);
                    System.exit(3);
                }
            }
        }
    }
}
