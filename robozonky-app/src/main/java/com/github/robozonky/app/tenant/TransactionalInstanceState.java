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

package com.github.robozonky.app.tenant;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

import com.github.robozonky.common.state.InstanceState;
import com.github.robozonky.common.state.StateModifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Delegates to default {@link InstanceState} implementation, except for {@link #update(Consumer)} and
 * {@link #reset(Consumer)}, which are stored for later.
 * @param <T>
 */
final class TransactionalInstanceState<T> implements InstanceState<T> {

    private static final Logger LOGGER = LogManager.getLogger(TransactionalInstanceState.class);

    private final Collection<Runnable> stateUpdates;
    private final InstanceState<T> parent;

    public TransactionalInstanceState(final Collection<Runnable> stateUpdates, final InstanceState<T> parent) {
        this.stateUpdates = stateUpdates;
        this.parent = parent;
    }

    @Override
    public void update(final Consumer<StateModifier<T>> modifier) {
        LOGGER.debug("Updating transactional instance state for {}.", parent);
        stateUpdates.add(() -> parent.update(modifier));
    }

    @Override
    public void reset(final Consumer<StateModifier<T>> setter) {
        LOGGER.debug("Resetting transactional instance state for {}.", parent);
        stateUpdates.add(() -> parent.reset(setter));
    }

    @Override
    public Optional<String> getValue(final String key) {
        return parent.getValue(key);
    }

    @Override
    public Stream<String> getKeys() {
        return parent.getKeys();
    }
}
