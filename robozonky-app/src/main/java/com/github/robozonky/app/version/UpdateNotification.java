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

package com.github.robozonky.app.version;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import com.github.robozonky.api.notifications.RoboZonkyExperimentalUpdateDetectedEvent;
import com.github.robozonky.api.notifications.RoboZonkyUpdateDetectedEvent;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.util.Refreshable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.github.robozonky.app.events.EventFactory.roboZonkyExperimentalUpdateDetected;
import static com.github.robozonky.app.events.EventFactory.roboZonkyUpdateDetected;

/**
 * When notified of a change in versions by {@link UpdateMonitor}, this class will determine whether or not these
 * versions represent a version update compared to the running version, and send out
 * {@link RoboZonkyUpdateDetectedEvent} or {@link RoboZonkyExperimentalUpdateDetectedEvent} events.
 */
class UpdateNotification implements Refreshable.RefreshListener<VersionIdentifier> {

    private static final Logger LOGGER = LoggerFactory.getLogger(UpdateNotification.class);

    private final String currentVersion;
    // the versions are cached; in case RefreshListener's valueUnset() ever happens, we want the values kept
    private final AtomicReference<String> lastKnownStableVersion = new AtomicReference<>(),
            lastKnownUnstableVersion = new AtomicReference<>();

    public UpdateNotification() {
        this(Defaults.ROBOZONKY_VERSION);
    }

    /**
     * This constructor only exists for testing purposes.
     * @param currentVersion The version to compare against.
     */
    UpdateNotification(final String currentVersion) {
        this.currentVersion = currentVersion;
    }

    /**
     * Execute an action when a new version is detected.
     * @param newVersion New version that has just been detected.
     * @param lastKnownVersion The version that was last known to exist. Will be updated.
     * @param handler Action to call.
     */
    private void updateVersion(final String newVersion, final AtomicReference<String> lastKnownVersion,
                               final Consumer<String> handler) {
        if (Objects.equals(lastKnownVersion.get(), newVersion)) { // nothing to do
            return;
        }
        lastKnownVersion.set(newVersion);
        if (VersionComparator.isSmallerThan(currentVersion, newVersion)) {
            handler.accept(newVersion);
        }
    }

    private void updateStableVersion(final String newVersion) {
        updateVersion(newVersion, lastKnownStableVersion, v -> {
            UpdateNotification.LOGGER.info("You are using an obsolete version of RoboZonky. Please upgrade to {}.",
                                           newVersion);
            Events.allSessions().fire(roboZonkyUpdateDetected(newVersion));
        });
    }

    private void updateUnstableVersion(final String newVersion) {
        updateVersion(newVersion, lastKnownUnstableVersion, v -> {
            UpdateNotification.LOGGER.info("Experimental version of RoboZonky is available. Try {} at your own risk.",
                                           newVersion);
            Events.allSessions().fire(roboZonkyExperimentalUpdateDetected(newVersion));
        });
    }

    @Override
    public void valueSet(final VersionIdentifier newVersion) {
        updateStableVersion(newVersion.getLatestStable());
        // the below will only happen if there is an unstable that is newer than the stable above
        newVersion.getLatestUnstable().ifPresent(this::updateUnstableVersion);
    }

    @Override
    public void valueChanged(final VersionIdentifier oldVersion, final VersionIdentifier newVersion) {
        valueSet(newVersion);
    }
}
