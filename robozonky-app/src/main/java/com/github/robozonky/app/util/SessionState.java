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

package com.github.robozonky.app.util;

import java.util.Collection;
import java.util.Collections;
import java.util.function.ToIntFunction;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import com.github.robozonky.internal.api.State;
import org.eclipse.collections.api.set.primitive.IntSet;
import org.eclipse.collections.api.set.primitive.MutableIntSet;
import org.eclipse.collections.impl.set.mutable.primitive.IntHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SessionState<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionState.class);
    private static final State.ClassSpecificState STATE = State.forClass(SessionState.class);
    private final MutableIntSet items;
    private final ToIntFunction<T> idSupplier;
    private final String key;

    public SessionState(final ToIntFunction<T> idSupplier, final String key) {
        this(Collections.emptyList(), idSupplier, key);
    }

    public SessionState(final Collection<T> retain, final ToIntFunction<T> idSupplier, final String key) {
        this.key = key;
        this.idSupplier = idSupplier;
        this.items = read();
        this.items.retainAll(retain.stream().mapToInt(idSupplier).toArray());
        SessionState.LOGGER.debug("'{}' contains {}.", key, items);
    }

    private MutableIntSet read() {
        final int[] result = SessionState.STATE.getValues(key)
                .map(s -> s.stream()
                        .mapToInt(Integer::parseInt)
                        .toArray())
                .orElse(new int[0]);
        return IntHashSet.newSetWith(result);
    }

    private void write(final IntSet items) {
        final Stream<String> result = IntStream.of(items.toArray()).mapToObj(String::valueOf);
        SessionState.STATE.newBatch().set(key, result).call();
    }

    public synchronized void put(final T item) {
        items.add(idSupplier.applyAsInt(item));
        write(items);
    }

    public synchronized boolean contains(final T item) {
        return items.contains(idSupplier.applyAsInt(item));
    }
}
