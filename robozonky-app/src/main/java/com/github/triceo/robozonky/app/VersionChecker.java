/*
 * Copyright 2017 Lukáš Petrovický
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
import java.util.function.Consumer;

import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.app.util.Scheduler;
import com.github.triceo.robozonky.app.version.VersionCheck;
import com.github.triceo.robozonky.app.version.VersionIdentifier;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executes a version check on the background.
 */
class VersionChecker implements ShutdownHook.Handler {

    private static final Logger LOGGER = LoggerFactory.getLogger(VersionChecker.class);

    @Override
    public Optional<Consumer<ShutdownHook.Result>> get() {
        final Refreshable<VersionIdentifier> r = VersionCheck.retrieveLatestVersion(Scheduler.BACKGROUND_SCHEDULER);
        return Optional.of((code) -> VersionChecker.newerRoboZonkyVersionExists(r));
    }

    static boolean newerRoboZonkyVersionExists(final VersionIdentifier version, final String currentVersion) {
        final String latestStable = version.getLatestStable();
        final boolean hasNewerStable = VersionCheck.isSmallerThan(currentVersion, latestStable);
        if (hasNewerStable) {
            VersionChecker.LOGGER.info("You are using an obsolete version of RoboZonky. Please upgrade to {}.",
                    latestStable);
            return true;
        }
        // check for latest unstable version
        final Optional<String> latestUnstable = version.getLatestUnstable();
        final boolean hasNewerUnstable =
                latestUnstable.isPresent() && VersionCheck.isSmallerThan(currentVersion, latestUnstable.get());
        if (hasNewerUnstable) {
            VersionChecker.LOGGER.info("You are using the latest stable version of RoboZonky.");
            VersionChecker.LOGGER.info("There is a new beta version of RoboZonky available. Try version {}, " +
                    " if you feel adventurous.", latestUnstable.get());
            return true;
        } else {
            VersionChecker.LOGGER.info("You are using the latest version of RoboZonky.");
            return false;
        }
    }

    private static boolean newerRoboZonkyVersionExists(final VersionIdentifier version) {
        return VersionChecker.newerRoboZonkyVersionExists(version, Defaults.ROBOZONKY_VERSION);
    }

    /**
     * Check the current version against a different version. Print log message with results.
     * @param version Version to check against.
     * @return True when a more recent version was found.
     */
    static boolean newerRoboZonkyVersionExists(final Refreshable<VersionIdentifier> version) {
        return version.getLatest().map(VersionChecker::newerRoboZonkyVersionExists).orElse(false);
    }

}
