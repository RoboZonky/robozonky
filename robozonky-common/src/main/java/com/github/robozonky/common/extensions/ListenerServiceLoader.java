/*
 * Copyright 2017 The RoboZonky Project
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

import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.EventListener;
import com.github.robozonky.api.notifications.ListenerService;
import com.github.robozonky.util.Scheduler;
import com.github.robozonky.util.StreamUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class ListenerServiceLoader {

    private static final Logger LOGGER = LoggerFactory.getLogger(ListenerServiceLoader.class);
    private static final ServiceLoader<ListenerService> LOADER =
            ExtensionsManager.INSTANCE.getServiceLoader(ListenerService.class);

    static <T extends Event> List<Refreshable<EventListener<T>>> load(final Class<T> eventType,
                                                                      final Iterable<ListenerService> loader,
                                                                      final Scheduler scheduler) {
        return StreamUtil.toStream(loader)
                .peek(s -> ListenerServiceLoader.LOGGER.debug("Processing '{}'.", s.getClass()))
                .map(s -> s.findListener(eventType))
                .filter(Objects::nonNull)
                .peek(scheduler::submit)
                .collect(Collectors.toList());
    }

    public static <T extends Event> List<Refreshable<EventListener<T>>> load(final Class<T> eventType) {
        return ListenerServiceLoader.load(eventType, ListenerServiceLoader.LOADER, Scheduler.inBackground());
    }
}
