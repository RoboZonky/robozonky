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

package com.github.robozonky.app;

import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyEnding;
import static com.github.robozonky.app.events.impl.EventFactory.roboZonkyInitialized;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static java.lang.System.lineSeparator;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.RoboZonkyEndingEvent;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.internal.Defaults;

/**
 * Will send {@link RoboZonkyInitializedEvent} immediately and {@link RoboZonkyEndingEvent} when it's time to shut down
 * the app.
 */
class RoboZonkyStartupNotifier implements ShutdownHook.Handler {

    private static final Logger LOGGER = LogManager.getLogger(RoboZonkyStartupNotifier.class);

    private final SessionInfo session;

    public RoboZonkyStartupNotifier(final SessionInfo session) {
        this.session = session;
    }

    private String replaceVersionPlaceholder(String source, String version) {
        var id = "v" + version;
        if (!session.getName()
            .isBlank()) {
            id = id + " '" + session.getName() + "'";
        }
        var replaced = source.replace("$VERSION", id);
        return String.format("#%79s", replaced.substring(1)
            .trim());
    }

    String readBanner(String version) {
        var banner = "robozonky-banner.txt";
        try (var reader = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(banner)))) {
            return reader.lines()
                .map(String::trim)
                .map(s -> replaceVersionPlaceholder(s, version))
                .collect(Collectors.joining(lineSeparator(), lineSeparator(), ""));
        } catch (Exception ex) {
            LOGGER.debug("Failed reading banner resource.", ex);
            return "===== RoboZonky v" + version + " '" + session.getName() + "' at your service! =====";
        }
    }

    @Override
    public Optional<Consumer<ReturnCode>> get() {
        var version = Defaults.ROBOZONKY_VERSION;
        LOGGER.info(readBanner(version));
        LOGGER.debug("Running {} {} v{} from {}.", getProperty("java.vm.vendor"), getProperty("java.vm.name"),
                getProperty("java.vm.version"), getProperty("java.home"));
        LOGGER.debug("Running on {} v{} ({}, {} CPUs, {}, {}).", getProperty("os.name"), getProperty("os.version"),
                getProperty("os.arch"), getRuntime().availableProcessors(), Locale.getDefault(),
                Charset.defaultCharset());
        LOGGER.debug("Current working directory is '{}'.", getProperty("user.dir"));
        if (session.isDryRun()) {
            LOGGER.info("RoboZonky is doing a dry run. It will not invest any real money.");
        }
        Events.global()
            .fire(roboZonkyInitialized());
        return Optional.of(result -> {
            final CompletableFuture<?> waitUntilFired = Events.global()
                .fire(roboZonkyEnding());
            try {
                LOGGER.debug("Waiting for events to be processed.");
                waitUntilFired.join();
            } catch (final Exception ex) {
                LOGGER.debug("Exception while waiting for the final event being processed.", ex);
            } finally {
                LOGGER.info("RoboZonky v{} '{}' out.", version, session.getName());
            }
        });
    }
}
