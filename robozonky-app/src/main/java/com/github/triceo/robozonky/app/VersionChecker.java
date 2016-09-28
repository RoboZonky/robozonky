/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import java.util.function.Supplier;

import com.github.triceo.robozonky.app.version.VersionCheck;
import com.github.triceo.robozonky.app.version.VersionIdentifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a version check on the background.
 */
class VersionChecker implements Supplier<Optional<Consumer<ReturnCode>>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionChecker.class);
    private final ExecutorService executor = Executors.newFixedThreadPool(1);

    @Override
    public Optional<Consumer<ReturnCode>> get() {
        final Future<VersionIdentifier> future = VersionCheck.retrieveLatestVersion(this.executor);
        return Optional.of((code) -> {
            VersionChecker.newerRoboZonkyVersionExists(future);
            this.executor.shutdown();
        });
    }

    /**
     * Check the current version against a different version. Print log message with results.
     * @param futureVersion Version to check against.
     * @return True when a more recent version was found.
     */
    static boolean newerRoboZonkyVersionExists(final Future<VersionIdentifier> futureVersion) {
        try {
            // check for latest stable version
            final VersionIdentifier version = futureVersion.get();
            final String latestStable = version.getLatestStable();
            final boolean hasNewerStable = VersionCheck.isCurrentVersionOlderThan(latestStable);
            if (hasNewerStable) {
                VersionChecker.LOGGER.info("You are using an obsolete version of RoboZonky. Please upgrade to {}.",
                        latestStable);
                return true;
            }
            // check for latest unstable version
            final Optional<String> latestUnstable = version.getLatestUnstable();
            final boolean hasNewerUnstable =
                    latestUnstable.isPresent() && VersionCheck.isCurrentVersionOlderThan(latestUnstable.get());
            if (hasNewerUnstable) {
                VersionChecker.LOGGER.info("You are using the latest stable version of RoboZonky.");
                VersionChecker.LOGGER.info("There is a new beta version of RoboZonky available. Try version {}, " +
                        " if you feel adventurous.", latestUnstable.get());
                return true;
            } else {
                VersionChecker.LOGGER.info("You are using the latest version of RoboZonky.");
                return false;
            }
        } catch (final InterruptedException | ExecutionException ex) {
            VersionChecker.LOGGER.trace("Version check failed.", ex);
            return false;
        }
    }

}
