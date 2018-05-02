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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import org.eclipse.collections.impl.list.mutable.FastList;

final class StateModifierImpl<T> implements StateModifier<T>,
                                            Callable<Boolean> {

    private final Collection<BiConsumer<StateStorage, String>> actions = new FastList<>(0),
            consistency = new FastList<>(0);
    private final InstanceStateImpl<T> state;

    public StateModifierImpl(final InstanceStateImpl<T> state, final boolean fresh) {
        this.state = state;
        if (fresh) { // first action is to reset the class-specific state
            actions.add(StateStorage::unsetValues);
            consistency.add(StateStorage::unsetValues);
        }
    }

    @Override
    public StateModifier<T> put(final String key, final String value) {
        actions.add((state, section) -> state.setValue(section, key, value));
        consistency.add((state, section) -> state.unsetValue(section, key)); // remove this from backup, as it's in main
        return this;
    }

    @Override
    public StateModifier<T> remove(final String key) {
        final BiConsumer<StateStorage, String> action = (state, section) -> state.unsetValue(section, key);
        actions.add(action);
        consistency.add(action); // also remove the item from backup, otherwise parent still sees it there and retrieves
        return this;
    }

    @Override
    public Boolean call() {
        final String sectionName = state.getSectionName();
        final StateStorage backend = state.getStorage();
        actions.forEach(a -> a.accept(backend, sectionName));
        backend.setValue(sectionName, StateReader.LAST_UPDATED_KEY, OffsetDateTime.now().toString());
        if (backend.store()) { // perform changes to backup if necessary
            final StateStorage underlying = state.getUnderlyingStorage();
            consistency.forEach(a -> a.accept(underlying, sectionName));
            return underlying.store();
        } else {
            return false;
        }
    }
}
