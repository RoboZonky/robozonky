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

package com.github.triceo.robozonky.operations;

import java.util.Optional;
import java.util.function.BiFunction;

import org.slf4j.Logger;

public abstract class BiOperation<In1, In2, Out> implements BiFunction<In1, In2, Optional<Out>> {

    protected abstract Logger getLogger();

    protected abstract Out perform(final In1 input1, final In2 input2);

    @Override
    public Optional<Out> apply(final In1 input1, final In2 input2) {
        this.getLogger().trace("Starting on input '{}', '{}'.", input1, input2);
        try {
            final Out result = this.perform(input1, input2);
            this.getLogger().trace("Succeeded with result '{}'.", result);
            return Optional.of(result);
        } catch (final Exception ex) {
            this.getLogger().warn("Failed, attempting to continue.", ex);
            return Optional.empty();
        }
    }

}
