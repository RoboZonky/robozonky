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

package com.github.robozonky.common.extensions;

import java.net.URL;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.common.state.TenantState;
import com.github.robozonky.internal.util.LazyInitialized;
import com.github.robozonky.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ListenerServiceLoader {

    private static final String CONFIG_LOCATION_PROPERTY = "configLocation";
    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerServiceLoader.class);
    private static final LazyInitialized<ServiceLoader<ListenerService>> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(ListenerService.class);

    private ListenerServiceLoader() {
        // no instances
    }

    /**
     * Retrieve the location configuration previously stored through
     * {@link #registerConfiguration(SessionInfo, URL)}.
     * @param session The tenant for which the information should be retrieved.
     * @return Empty if not registered.
     */
    public static Optional<String> getNotificationConfiguration(final SessionInfo session) {
        return TenantState.of(session)
                .in(ListenerService.class)
                .getValue(CONFIG_LOCATION_PROPERTY);
    }

    /**
     * Make sure the location for notifications configuration is stored for a given tenant. This is to work around the
     * fact that there is no way how to pass properties to robozonky-notifications, due to them being service-loaded.
     * @param username Tenant in question.
     * @param configurationLocation Location of notification configuration.
     */
    public static void registerConfiguration(final String username, final URL configurationLocation) {
        registerConfiguration(new SessionInfo(username), configurationLocation);
    }

    /**
     * Make sure the location for notifications configuration is stored for a given tenant. This is to work around the
     * fact that there is no way how to pass properties to robozonky-notifications, due to them being service-loaded.
     * @param session Tenant in question.
     * @param configurationLocation Location of notification configuration.
     */
    public static void registerConfiguration(final SessionInfo session, final URL configurationLocation) {
        registerConfiguration(session, configurationLocation.toExternalForm());
    }

    /**
     * Make sure the location for notifications configuration is stored for a given tenant. This is to work around the
     * fact that there is no way how to pass properties to robozonky-notifications, due to them being service-loaded.
     * @param session Tenant in question.
     * @param configurationLocation Location of notification configuration.
     */
    public static void registerConfiguration(final SessionInfo session,
                                             final String configurationLocation) {
        LOGGER.debug("Tenant '{}' notification configuration: '{}'.", session.getUsername(), configurationLocation);
        TenantState.of(session)
                .in(ListenerService.class)
                .update(state -> state.put(CONFIG_LOCATION_PROPERTY, configurationLocation));
    }

    /**
     * Make sure the location for notifications configuration is not stored for a given tenant.
     * @param session Tenant in question.
     */
    public static void unregisterConfiguration(final SessionInfo session) {
        TenantState.of(session)
                .in(ListenerService.class)
                .update(state -> state.remove(CONFIG_LOCATION_PROPERTY));
        LOGGER.debug("Tenant '{}' notification configuration deleted.", session.getUsername());
    }

    static <T extends Event> List<EventListenerSupplier<T>> load(final Class<T> eventType,
                                                                 final Iterable<ListenerService> loader) {
        LOGGER.debug("Loading listeners for {}.", eventType);
        return StreamUtil.toStream(loader)
                .peek(s -> ListenerServiceLoader.LOGGER.debug("Processing '{}'.", s.getClass()))
                .flatMap(s -> s.findListeners(eventType))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    public static <T extends Event> List<EventListenerSupplier<T>> load(final Class<T> eventType) {
        return ListenerServiceLoader.load(eventType, ListenerServiceLoader.LOADER.get());
    }
}
