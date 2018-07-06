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
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class Main implements Function<Feature, Integer> {

    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(final String... args) {
        LOGGER.info("Welcome to the RoboZonky command-line configuration and validation tool.");
        LOGGER.warn("This is a tool for the brave. Create a backup copy of RoboZonky or " +
                            "use RoboZonky installer instead.");
        final Optional<Feature> featureOptional = CommandLine.parse(args);
        featureOptional.ifPresent((feature) -> {
            final int exitCode = new Main().apply(feature);
            System.exit(exitCode);
        });
        // something is wrong, call for help
        CommandLine.parse("-h");
        System.exit(1);
    }

    @Override
    public Integer apply(final Feature feature) {
        if (feature instanceof HelpFeature) {
            return 1;
        } else {
            try {
                LOGGER.info("--- Press any key to run: '{}'", feature.describe());
                try {
                    System.in.read();
                } catch (final IOException ex) {
                    throw new SetupFailedException("Exception while waiting for keypress.", ex);
                }
                feature.setup();
                LOGGER.info("--- Executed, running test of the new setup.");
                feature.test();
                LOGGER.info("--- Success.");
                return 0;
            } catch (final SetupFailedException e) {
                LOGGER.warn("!!! Could not perform setup, configuration may have been corrupted.", e);
                return 2;
            } catch (final TestFailedException e) {
                LOGGER.warn("!!! Could not test setup, configuration may have been corrupted.", e);
                return 3;
            }
        }
    }
}
