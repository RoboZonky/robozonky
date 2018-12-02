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

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;

import com.github.robozonky.internal.util.DateUtil;

final class StateModifierImpl<T> implements StateModifier<T>,
                                            Callable<Boolean> {

    private final Collection<BiConsumer<StateStorage, String>> actions = new ArrayList<>(0);
    private final InstanceStateImpl<T> instanceState;

    public StateModifierImpl(final InstanceStateImpl<T> instanceState, final boolean fresh) {
        this.instanceState = instanceState;
        if (fresh) { // first action is to reset the class-specific state
            actions.add(StateStorage::unsetValues);
        }
    }

    @Override
    public StateModifier<T> put(final String key, final String value) {
        actions.add((state, section) -> state.setValue(section, key, value));
        return this;
    }

    @Override
    public StateModifier<T> remove(final String key) {
        final BiConsumer<StateStorage, String> action = (state, section) -> state.unsetValue(section, key);
        actions.add(action);
        return this;
    }

    @Override
    public Boolean call() {
        final String sectionName = instanceState.getSectionName();
        final StateStorage backend = instanceState.getStorage();
        actions.forEach(a -> a.accept(backend, sectionName));
        backend.setValue(sectionName, Constants.LAST_UPDATED_KEY.getValue(), DateUtil.offsetNow().toString());
        return backend.store();
    }
}
