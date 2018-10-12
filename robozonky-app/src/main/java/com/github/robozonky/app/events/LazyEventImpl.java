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

package com.github.robozonky.app.events;

import java.util.function.Supplier;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.internal.util.LazyInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class LazyEventImpl<T extends Event> implements LazyEvent<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(LazyEventImpl.class);

    private final Class<T> eventType;
    private final LazyInitialized<T> supplier;

    public LazyEventImpl(final Class<T> eventType, final Supplier<T> eventSupplier) {
        this.eventType = eventType;
        this.supplier = LazyInitialized.create(() -> {
            LOGGER.trace("Instantiating {}.", eventType);
            final T result = eventSupplier.get();
            LOGGER.trace("Instantiated to {}.", result);
            return result;
        });
    }

    @Override
    public Class<T> getEventType() {
        return eventType;
    }

    @Override
    public T get() {
        return supplier.get();
    }

    @Override
    public String toString() {
        return "LazyEventImpl{" +
                "eventType=" + eventType +
                '}';
    }
}
