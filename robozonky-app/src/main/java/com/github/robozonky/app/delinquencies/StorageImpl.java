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

package com.github.robozonky.app.delinquencies;

import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.LongStream;
import java.util.stream.Stream;

import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.state.InstanceState;

final class StorageImpl implements Storage {

    private final InstanceState<StorageImpl> state;
    private final String key;
    private final Set<Long> originalContents;
    private final Set<Long> toAdd = new TreeSet<>();
    private final Set<Long> toRemove = new TreeSet<>();

    public StorageImpl(final Tenant tenant, final String key) {
        this.state = tenant.getState(StorageImpl.class);
        this.key = key;
        this.originalContents = state.getValues(key)
                .orElse(Stream.empty())
                .map(Long::parseLong)
                .collect(Collectors.toSet());
    }

    @Override
    public synchronized boolean isKnown(final long investmentId) {
        return originalContents.contains(investmentId);
    }

    @Override
    public synchronized boolean add(final long investmentId) {
        toRemove.remove(investmentId);
        if (originalContents.contains(investmentId)) {
            return false;
        } else {
            return toAdd.add(investmentId);
        }
    }

    @Override
    public synchronized boolean remove(final long investmentId) {
        toAdd.remove(investmentId);
        if (originalContents.contains(investmentId)) {
            return toRemove.add(investmentId);
        } else {
            return false;
        }
    }

    @Override
    public synchronized void persist() {
        if (toAdd.isEmpty() && toRemove.isEmpty()) {
            return;
        }
        originalContents.addAll(toAdd);
        originalContents.removeAll(toRemove);
        final Stream<String> result = originalContents.stream()
                .distinct()
                .sorted()
                .map(String::valueOf);
        state.update(m -> m.put(key, result));
    }

    @Override
    public LongStream complement(final Set<Long> investmentIds) {
        return originalContents.stream()
                .filter(i -> !investmentIds.contains(i))
                .mapToLong(i -> i);
    }
}
