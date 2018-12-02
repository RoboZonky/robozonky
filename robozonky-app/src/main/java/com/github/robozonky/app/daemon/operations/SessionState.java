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

package com.github.robozonky.app.daemon.operations;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.state.InstanceState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The purpose of this class is to keep a certain number of elements, and to persist those across many repeated
 * runs of RoboZonky. This is primarily used to store information to facilitate a dry run.
 * <p>
 * For example, when selling participations, the sell commands don't actually make it to Zonky. (Dry run!) So, for the
 * robot to not attempt to sell the same participation over and over again, the participation will be stored here and
 * kept for eternity.
 * @param <T> Type of elements to store.
 */
@SuppressWarnings("rawtypes")
final class SessionState<T> {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionState.class);
    private static final long[] NO_LONGS = new long[0];
    private final Collection<Long> items;
    private final ToLongFunction<T> idSupplier;
    private final String key;
    private final InstanceState<SessionState> state;

    /**
     * Create an empty instance.
     * @param tenant Session identifier.
     * @param idSupplier Supplies ID of the element to be stored, which will be used as a traditional primary key.
     * @param key Name of this collection elements. Different instances with the same value operate on the same
     * underlying storage.
     */
    public SessionState(final Tenant tenant, final ToLongFunction<T> idSupplier, final String key) {
        this(tenant, Collections.emptyList(), idSupplier, key);
    }

    /**
     * Create an instance, potentially retaining some elements from the underlying storage.
     * @param tenant Session identifier.
     * @param retain Elements to retain.
     * @param idSupplier Supplies ID of the element to be stored, which will be used as a traditional primary key.
     * @param key Name of this collection elements. Different instances with the same value operate on the same
     * underlying storage.
     */
    public SessionState(final Tenant tenant, final Collection<T> retain, final ToLongFunction<T> idSupplier,
                        final String key) {
        this.state = tenant.getState(SessionState.class);
        this.key = key;
        this.idSupplier = idSupplier;
        this.items = read();
        this.items.retainAll(retain.stream().mapToLong(idSupplier).boxed().collect(Collectors.toSet()));
        SessionState.LOGGER.debug("'{}' contains {}.", key, items);
    }

    private Set<Long> read() {
        final long[] result = state.getValues(key)
                .map(s -> s.mapToLong(Long::parseLong).toArray())
                .orElse(NO_LONGS);
        SessionState.LOGGER.trace("'{}' read {}.", key, result);
        return LongStream.of(result).boxed().collect(Collectors.toSet());
    }

    private void write(final Collection<Long> items) {
        state.update(b -> b.put(key, items.stream().map(String::valueOf)));
        final String value = state.getValue(key).orElse("nothing");
        SessionState.LOGGER.trace("'{}' wrote '{}'.", key, value);
    }

    /**
     * Immediately writes the item to the underlying storage.
     * @param item
     */
    public synchronized void put(final T item) {
        items.add(idSupplier.applyAsLong(item));
        write(items);
    }

    /**
     * Whether or not an item is contained in the underlying storage.
     * @param item
     */
    public synchronized boolean contains(final T item) {
        return items.contains(idSupplier.applyAsLong(item));
    }
}
