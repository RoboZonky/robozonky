/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.internal.async;

/**
 * Listener for changes to the original resource. Use {@link #registerListener(ChangeListener)} to
 * enable. Implementations of methods in this interface must not throw exceptions.
 * @param <T> Target {@link Refreshable}'s generic type.
 */
public interface ChangeListener<T> {

    /**
     * Resource now has a value where there was none before.
     * @param newValue New value for the resource.
     */
    default void valueSet(final T newValue) {
        // do nothing
    }

    /**
     * Resource used to have a value but no longer has one.
     * @param oldValue Former value of the resource.
     */
    default void valueUnset(final T oldValue) {
        // do nothing
    }

}
