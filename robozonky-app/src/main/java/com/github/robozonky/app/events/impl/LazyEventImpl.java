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

package com.github.robozonky.app.events.impl;

import java.time.OffsetDateTime;
import java.util.function.Supplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.internal.tenant.LazyEvent;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.functional.Memoizer;

final class LazyEventImpl<T extends Event> implements LazyEvent<T> {

    private static final Logger LOGGER = LogManager.getLogger(LazyEventImpl.class);

    private final Class<T> eventType;
    private final Supplier<T> supplier;

    public LazyEventImpl(final Class<T> eventType, final Supplier<T> eventSupplier) {
        this.eventType = eventType;
        final OffsetDateTime conceivedOn = DateUtil.offsetNow();
        this.supplier = Memoizer.memoize(() -> {
            LOGGER.trace("Instantiating {}.", eventType);
            final T result = eventSupplier.get();
            ((AbstractEventImpl) result).setConceivedOn(conceivedOn); // make the event aware of when it was requested
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
