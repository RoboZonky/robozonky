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

package com.github.robozonky.app.management;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.EventListenerSupplier;
import com.github.robozonky.api.notifications.ExecutionCompletedEvent;
import com.github.robozonky.api.notifications.ListenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmxListenerService implements ListenerService {

    private static final Logger LOGGER = LoggerFactory.getLogger(JmxListenerService.class);
    private static Map<MBean, Object> IMPLEMENTATIONS = Collections.emptyMap();

    public static void setInstances(final Map<MBean, Object> implementations) {
        LOGGER.trace("Setting MBeans: {}.", implementations);
        IMPLEMENTATIONS = Collections.unmodifiableMap(implementations);
    }

    static Optional<Object> getMBean(final MBean type) {
        return Optional.ofNullable(IMPLEMENTATIONS.get(type));
    }

    private static void callOnRuntime(final Consumer<Runtime> operation) {
        final Optional<Object> mbean = getMBean(MBean.RUNTIME);
        if (mbean.isPresent()) {
            operation.accept((Runtime) mbean.get());
        } else {
            LOGGER.warn("Runtime MBean not found.");
        }
    }

    private static <T extends Event> EventListener<T> newListener(final Class<T> eventType) {
        if (ExecutionCompletedEvent.class.isAssignableFrom(eventType)) {
            return (event, sessionInfo) -> {
                final ExecutionCompletedEvent evt = (ExecutionCompletedEvent) event;
                callOnRuntime(bean -> bean.handle(evt));
            };
        } else {
            return null;
        }
    }

    @Override
    public <T extends Event> Stream<EventListenerSupplier<T>> findListeners(final Class<T> eventType) {
        final EventListener<T> listener = JmxListenerService.newListener(eventType);
        if (listener == null) {
            return Stream.empty();
        }
        return Stream.of(() -> Optional.of(listener));
    }
}
