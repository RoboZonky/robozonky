/*
 * Copyright 2020 The RoboZonky Project
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
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.notifications.GlobalEvent;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.app.events.impl.EventFactory;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.jobs.SimplePayload;
import com.github.robozonky.internal.util.functional.Either;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class VersionDetection implements SimplePayload {

    private static final Logger LOGGER = LogManager.getLogger(VersionDetection.class);

    private final Supplier<Either<Throwable, Response>> metadata;
    private final AtomicReference<String> lastKnownStableVersion = new AtomicReference<>();
    private final AtomicReference<String> lastKnownExperimentalVersion = new AtomicReference<>();

    public VersionDetection() {
        this(() -> new MavenMetadataParser().apply(Defaults.ROBOZONKY_VERSION));
    }

    VersionDetection(final Supplier<Either<Throwable, Response>> metadata) {
        this.metadata = metadata;
    }

    private static void processVersion(Optional<String> version, AtomicReference<String> target, String unchanged,
                                       String changed, Function<String, ? extends GlobalEvent> eventSupplier) {
        version.ifPresentOrElse(newVersion -> {
            var oldVersion = target.getAndSet(newVersion);
            if (Objects.equals(newVersion, oldVersion)) {
                LOGGER.debug(unchanged, newVersion);
                return;
            }
            LOGGER.info(changed, newVersion);
            Events.global().fire(eventSupplier.apply(newVersion));
        }, () -> target.set(null));
    }

    @Override
    public void run() {
        final Either<Throwable, Response> result = metadata.get();
        if (result.isLeft()) {
            LOGGER.debug("Failed retrieving RoboZonky version information.", result.getLeft());
            return;
        }
        final Response currentResponse = result.get();
        processVersion(currentResponse.getMoreRecentStableVersion(), lastKnownStableVersion,
                       "Latest stable version unchanged: {}.",
                       "You are using an obsolete version of RoboZonky. Please upgrade to {}.",
                       EventFactory::roboZonkyUpdateDetected);
        processVersion(currentResponse.getMoreRecentExperimentalVersion(), lastKnownExperimentalVersion,
                       "Latest experimental version unchanged: {}.",
                       "Experimental version of RoboZonky is available. Try {} at your own risk.",
                       EventFactory::roboZonkyExperimentalUpdateDetected);
    }
}
