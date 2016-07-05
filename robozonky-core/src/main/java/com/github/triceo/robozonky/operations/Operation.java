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
import java.util.function.Function;

import org.slf4j.Logger;

public abstract class Operation<In, Out> implements Function<In, Optional<Out>> {

    protected abstract Logger getLogger();

    protected abstract Out perform(final In input);

    @Override
    public Optional<Out> apply(final In input) {
        this.getLogger().trace("Starting on input '{}'.", input);
        try {
            final Out result = this.perform(input);
            this.getLogger().trace("Succeeded with result '{}'.", result);
            return Optional.of(result);
        } catch (final Exception ex) {
            this.getLogger().error("Failed.", ex);
            return Optional.empty();
        }
    }

}
