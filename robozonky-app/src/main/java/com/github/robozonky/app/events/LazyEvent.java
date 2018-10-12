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
import com.github.robozonky.api.notifications.EventListenerSupplier;

/**
 * Encapsulates the creation of {@link Event}. This is useful in cases where the creation of events is expensive, such
 * as when additional remote information needs to be retrieved or calculations performed. We can first determine whether
 * someone is actually listening for the event first, and only then perform these heavy operations.
 * @param <T>
 */
public interface LazyEvent<T extends Event> extends Supplier<T> {

    /**
     * Type of the event so that we can pre-scan all the {@link EventListenerSupplier}s.
     * @return
     */
    Class<T> getEventType();

    /**
     * Instantiate the event. This may result in expensive calls and should therefore only be performed when we are
     * sure we need the {@link Event} instance. The instantiation process only happens once, every subsequent invocation
     * must return the same object.
     * @return Instantiated instance.
     */
    @Override
    T get();
}
