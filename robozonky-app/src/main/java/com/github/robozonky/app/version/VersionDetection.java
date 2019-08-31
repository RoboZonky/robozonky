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

package com.github.robozonky.app.version;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

import com.github.robozonky.app.events.Events;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.jobs.SimplePayload;
import io.vavr.control.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyExperimentalUpdateDetected;
import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyUpdateDetected;

final class VersionDetection implements SimplePayload {

    private static final Logger LOGGER = LogManager.getLogger(VersionDetection.class);

    private final Supplier<Either<Throwable, Response>> metadata;
    private final AtomicReference<String> lastKnownStableVersion = new AtomicReference<>();
    private final AtomicReference<String> lastKnownExperimentalVersion = new AtomicReference<>();

    public VersionDetection() {
        this(() -> new MarvenMetadataParser().apply(Defaults.ROBOZONKY_VERSION));
    }

    VersionDetection(final Supplier<Either<Throwable, Response>> metadata) {
        this.metadata = metadata;
    }

    @Override
    public void run() {
        final Either<Throwable, Response> result = metadata.get();
        if (result.isLeft()) {
            LOGGER.debug("Failed retrieving RoboZonky version information.", result.getLeft());
            return;
        }
        final Response currentResponse = result.get();
        currentResponse.getMoreRecentStableVersion()
                .ifPresentOrElse(newVersion -> {
                    final String oldVersion = lastKnownStableVersion.getAndSet(newVersion);
                    if (Objects.equals(newVersion, oldVersion)) {
                        LOGGER.debug("Latest stable version unchanged: {}.", newVersion);
                        return;
                    }
                    LOGGER.info("You are using an obsolete version of RoboZonky. Please upgrade to {}.", newVersion);
                    Events.global().fire(roboZonkyUpdateDetected(newVersion));
                }, () -> lastKnownStableVersion.set(null));
        currentResponse.getMoreRecentExperimentalVersion()
                .ifPresentOrElse(newVersion -> {
                    final String oldVersion = lastKnownExperimentalVersion.getAndSet(newVersion);
                    if (Objects.equals(newVersion, oldVersion)) {
                        LOGGER.debug("Latest experimental version unchanged: {}.", newVersion);
                        return;
                    }
                    LOGGER.info("Experimental version of RoboZonky is available. Try {} at your own risk.", newVersion);
                    Events.global().fire(roboZonkyExperimentalUpdateDetected(newVersion));
                }, () -> lastKnownExperimentalVersion.set(null));
    }
}
