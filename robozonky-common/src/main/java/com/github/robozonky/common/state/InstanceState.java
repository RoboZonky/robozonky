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

package com.github.robozonky.common.state;

import java.util.function.Consumer;

/**
 * Represents state data specific to a given tenant and a given {@link Class}.
 * @param <T> Class for which the state is kept.
 */
public interface InstanceState<T> extends StateReader {

    /**
     * Perform updates to state using a given modifier. Will only actually be performed after the modifier finishes.
     * @param modifier Modifier to use.
     */
    void update(Consumer<StateModifier<T>> modifier);

    /**
     * Perform updates to state using a given setter, deleting all pre-existing state information. Will only actually
     * be performed after the setter finishes.
     * @param setter Modifier to use.
     */
    void reset(Consumer<StateModifier<T>> setter);

    /**
     * Delete all pre-existing information kept here.
     */
    default void reset() {
        reset(b -> {
            // do not add any content
        });
    }
}
