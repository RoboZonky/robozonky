/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications;

import java.util.Optional;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.triceo.robozonky.api.notifications.Event;
import com.github.triceo.robozonky.api.notifications.EventListener;
import com.github.triceo.robozonky.api.notifications.ListenerService;

class ListenerServiceLoader {

    private static final ServiceLoader<ListenerService> LOADER = ServiceLoader.load(ListenerService.class);

    private static <T> Stream<T> iteratorToStream(final Iterable<T> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    static <T extends Event> Set<EventListener<T>> load(final Class<T> eventType) {
        return ListenerServiceLoader.iteratorToStream(ListenerServiceLoader.LOADER)
                .parallel()
                .map(s -> s.findListener(eventType))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toSet());
    }

}
