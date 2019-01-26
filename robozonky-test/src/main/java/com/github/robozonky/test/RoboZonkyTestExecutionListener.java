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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.platform.engine.TestExecutionResult;
import org.junit.platform.launcher.TestExecutionListener;
import org.junit.platform.launcher.TestIdentifier;

public final class RoboZonkyTestExecutionListener implements TestExecutionListener {

    private static final Logger LOGGER = LogManager.getLogger(RoboZonkyTestExecutionListener.class);

    private static String identifyTest(final TestIdentifier testIdentifier) {
        return testIdentifier.getUniqueId();
    }

    @Override
    public void executionSkipped(final TestIdentifier testIdentifier, final String reason) {
        final String id = identifyTest(testIdentifier);
        LOGGER.info("Skipped {}. Cause: {}.", id, reason);
    }

    @Override
    public void executionStarted(final TestIdentifier testIdentifier) {
        final String id = identifyTest(testIdentifier);
        LOGGER.info("Started {}.", id);
    }

    @Override
    public void executionFinished(final TestIdentifier testIdentifier, final TestExecutionResult testExecutionResult) {
        final String id = identifyTest(testIdentifier);
        LOGGER.info("Finished {}.", id);
    }
}
