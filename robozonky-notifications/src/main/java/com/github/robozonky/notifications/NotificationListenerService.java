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
import java.util.concurrent.atomic.AtomicBoolean;
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

    private static final Map<String, RefreshableConfigStorage> PROPERTIES = UnifiedMap.newMap(0);
    private static final AtomicBoolean IS_INITIALIZED = new AtomicBoolean(false);

    static void initialize(final String username, final String configLocation) {
        try {
            final URL config = new URL(configLocation);
            LOGGER.debug("Initializing notifications for tenant '{}' from {}.", username, config);
            final RefreshableConfigStorage props = new RefreshableConfigStorage(config);
            Scheduler.inBackground().submit(props, Settings.INSTANCE.getRemoteResourceRefreshInterval());
            PROPERTIES.put(username, props);
        } catch (final MalformedURLException ex) {
            LOGGER.warn("Wrong notification configuration location for tenant '{}'.", username, ex);
        }
    }

    static void initialize() {
        TenantState.getKnownTenants().stream()
                .map(SessionInfo::new)
                .forEach(sessionInfo -> {
                    final Optional<String> value = ListenerServiceLoader.getNotificationConfiguration(sessionInfo);
                    final String username = sessionInfo.getUsername();
                    if (value.isPresent()) {
                        initialize(username, value.get());
                    } else {
                        LOGGER.debug("Not enabling notifications for tenant '{}'.", username);
                    }
                });
    }

    @Override
    public <T extends Event> Stream<EventListenerSupplier<T>> findListeners(final Class<T> eventType) {
        if (!IS_INITIALIZED.getAndSet(true)) {
            initialize();
        }
        final NotificationEventListenerSupplier<T> l = new NotificationEventListenerSupplier<>(eventType);
        PROPERTIES.values().forEach(v -> v.registerListener(l));
        return Stream.of(Target.values())
                .map(l::get)
                .map(e -> () -> e);
    }
}
