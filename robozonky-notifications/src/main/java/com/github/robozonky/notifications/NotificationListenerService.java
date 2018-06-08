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

package com.github.robozonky.notifications;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.util.Scheduler;
import org.eclipse.collections.impl.map.mutable.UnifiedMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class NotificationListenerService implements ListenerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationListenerService.class);

    private final Map<String, RefreshableConfigStorage> CONFIGURATIONS = UnifiedMap.newMap(0);

    Optional<RefreshableConfigStorage> readConfig(final String configLocation) {
        try {
            final URL config = new URL(configLocation);
            return Optional.of(new RefreshableConfigStorage(config));
        } catch (final MalformedURLException ex) {
            LOGGER.warn("Wrong notification configuration location.", ex);
            return Optional.empty();
        }
    }

    synchronized Optional<RefreshableConfigStorage> getTenantConfigurations(final SessionInfo sessionInfo) {
        final String username = sessionInfo.getUsername();
        if (!CONFIGURATIONS.containsKey(username)) { // already initialized
            final Optional<String> value = ListenerServiceLoader.getNotificationConfiguration(sessionInfo);
            if (!value.isPresent()) {
                LOGGER.debug("Not enabling notifications for tenant '{}'.", username);
                return Optional.empty();
            }
            final String config = value.get();
            LOGGER.debug("Initializing notifications for tenant '{}' from {}.", username, config);
            readConfig(config).ifPresent(props -> CONFIGURATIONS.put(username, props));
        }
        return Optional.ofNullable(CONFIGURATIONS.get(username));
    }

    Stream<RefreshableConfigStorage> getTenantConfigurations() {
        return TenantState.getKnownTenants().stream()
                .map(SessionInfo::new)
                .map(this::getTenantConfigurations)
                .flatMap(o -> o.map(Stream::of).orElse(Stream.empty()))
                .peek(c -> Scheduler.inBackground().submit(c, Settings.INSTANCE.getRemoteResourceRefreshInterval()));
    }

    @Override
    public <T extends Event> Stream<EventListenerSupplier<T>> findListeners(final Class<T> eventType) {
        final NotificationEventListenerSupplier<T> l = new NotificationEventListenerSupplier<>(eventType);
        final long tenants = getTenantConfigurations()
                .peek(config -> config.registerListener(l))
                .count();
        if (tenants > 0) {
            return Stream.of(Target.values()).map(e -> () -> l.apply(e));
        } else {
            return Stream.empty();
        }
    }
}
