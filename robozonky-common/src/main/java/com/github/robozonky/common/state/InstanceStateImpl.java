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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;

final class InstanceStateImpl<T> implements InstanceState<T> {

    private final String sectionName;
    private final StateStorage current;
    private final TenantState parent;

    public InstanceStateImpl(final TenantState parent, final String sectionName, final StateStorage current) {
        this.parent = parent;
        this.sectionName = sectionName;
        this.current = current;
    }

    private void execute(final StateModifierImpl<T> modifier) {
        synchronized (parent) {
            parent.assertNotDestroyed();
            modifier.call();
        }
    }

    private void execute(final Consumer<StateModifier<T>> modifier, final boolean fresh) {
        final StateModifierImpl<T> b = new StateModifierImpl<>(this, fresh);
        modifier.accept(b);
        execute(b);
    }

    @Override
    public void update(final Consumer<StateModifier<T>> modifier) {
        execute(modifier, false);
    }

    @Override
    public void reset(final Consumer<StateModifier<T>> modifier) {
        execute(modifier, true);
    }

    String getSectionName() {
        return sectionName;
    }

    StateStorage getStorage() {
        return current;
    }

    @Override
    public Optional<String> getValue(final String key) {
        parent.assertNotDestroyed();
        return current.getValue(sectionName, key);
    }

    @Override
    public Stream<String> getKeys() {
        parent.assertNotDestroyed();
        return current.getKeys(sectionName)
                .filter(s -> !Objects.equals(s, Constants.LAST_UPDATED_KEY.getValue()));
    }

    @Override
    public String toString() {
        return "InstanceStateImpl{" +
                "sectionName='" + sectionName + '\'' +
                '}';
    }
}
