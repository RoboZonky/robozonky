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

package com.github.robozonky.notifications;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.internal.async.Reloadable;
import com.github.robozonky.internal.extensions.ListenerServiceLoader;
import com.github.robozonky.internal.util.UrlUtil;

public final class NotificationListenerService implements ListenerService {

    private static final Logger LOGGER = LogManager.getLogger(NotificationListenerService.class);

    private final Map<String, Reloadable<ConfigStorage>> configurations = new HashMap<>(0);

    private static ConfigStorage retrieve(final URL source) {
        LOGGER.debug("Reading notification configuration from '{}'.", source);
        try (var inputStream = UrlUtil.open(source)
            .getInputStream()) {
            return ConfigStorage.create(inputStream);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed reading notification configuration from " + source, ex);
        }
    }

    private Optional<Reloadable<ConfigStorage>> readConfig(final String configLocation) {
        try {
            var config = new URL(configLocation);
            var configStorage = Reloadable.with(() -> retrieve(config))
                .reloadAfter(Duration.ofHours(1))
                .async()
                .build();
            return Optional.of(configStorage);
        } catch (final MalformedURLException ex) {
            LOGGER.warn("Wrong notification configuration location.", ex);
            return Optional.empty();
        }
    }

    private synchronized Optional<Reloadable<ConfigStorage>> getTenantConfiguration(final SessionInfo sessionInfo) {
        var username = sessionInfo.getUsername();
        if (!configurations.containsKey(username)) { // already initialized
            var maybeConfig = ListenerServiceLoader.getNotificationConfiguration(sessionInfo);
            if (maybeConfig.isEmpty()) {
                LOGGER.debug("Notifications disabled for '{}'.", username);
                return Optional.empty();
            }
            maybeConfig.flatMap(this::readConfig)
                .ifPresent(props -> {
                    LOGGER.debug("Initializing notifications for '{}'.", username);
                    configurations.put(username, props);
                });
        }
        return Optional.ofNullable(configurations.get(username));
    }

    private <T extends Event> EventListenerSupplier<T> getEventListenerSupplier(final SessionInfo sessionInfo,
            final Class<T> eventType,
            final Target target) {
        var listenerSupplier = new NotificationEventListenerSupplier<>(eventType);
        return () -> {
            getTenantConfiguration(sessionInfo)
                .map(reloadable -> reloadable.get()
                    .fold(ex -> null, Function.identity()))
                .ifPresentOrElse(listenerSupplier::configure, listenerSupplier::disable);
            return Optional.ofNullable(listenerSupplier.apply(target));
        };
    }

    @Override
    public <T extends Event> Stream<EventListenerSupplier<T>> findListeners(final SessionInfo sessionInfo,
            final Class<T> eventType) {
        return Stream.of(Target.values())
            .map(target -> getEventListenerSupplier(sessionInfo, eventType, target));
    }
}
