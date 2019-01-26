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

package com.github.robozonky.cli;

import java.io.IOException;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

abstract class AbstractFeature implements Feature {

    protected final Logger logger = LogManager.getLogger(this.getClass());

    @Override
    public ExitCode call() {
        logger.info("Welcome to the RoboZonky command-line configuration and validation tool.");
        logger.warn("This is a tool for the brave. Create a backup copy of RoboZonky " +
                            "or use RoboZonky installer instead.");
        try {
            final String description = describe();
            logger.info("--- Press any key to run: '{}'", description);
            System.in.read();
            setup();
            logger.info("--- Executed, running test of the new setup.");
            test();
            logger.info("--- Success.");
            return ExitCode.SUCCESS;
        } catch (final SetupFailedException | IOException e) {
            logger.error("Could not perform setup, configuration may have been corrupted.", e);
            return ExitCode.SETUP_FAIL;
        } catch (final TestFailedException e) {
            logger.error("Could not test setup, configuration may have been corrupted.", e);
            return ExitCode.TEST_FAIL;
        }
    }
}
