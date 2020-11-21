/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.state.InstanceState;
import com.github.robozonky.internal.tenant.Tenant;

/**
 * The purpose of this class is to keep a certain number of elements, and to persist those across many repeated
 * runs of RoboZonky.
 * This is used to store information to facilitate a dry run.
 * <p>
 * For example, when selling participations, the sell commands don't actually make it to Zonky. (Dry run!)
 * So, for the robot to not attempt to sell the same participation over and over again,
 * the participation ID will be stored here and kept for eternity.
 * 
 * @param <T> Type of elements to store.
 */
@SuppressWarnings("rawtypes")
final class SessionState<T> {

    private static final Logger LOGGER = LogManager.getLogger(SessionState.class);
    private static final long[] NO_LONGS = new long[0];
    private final boolean isEnabled;
    private final Collection<Long> items;
    private final ToLongFunction<T> idSupplier;
    private final String key;
    private final InstanceState<SessionState> state;

    /**
     * Create an empty instance.
     * 
     * @param tenant     Session identifier.
     * @param idSupplier Supplies ID of the element to be stored, which will be used as a traditional primary key.
     * @param key        Name of this collection elements. Different instances with the same value operate on the same
     *                   underlying storage.
     */
    public SessionState(final Tenant tenant, final ToLongFunction<T> idSupplier, final String key) {
        this.isEnabled = tenant.getSessionInfo()
            .isDryRun();
        this.state = tenant.getState(SessionState.class);
        this.key = key;
        this.idSupplier = idSupplier;
        this.items = isEnabled ? read() : Collections.emptyList(); // Only do work in dry run.
        LOGGER.debug("'{}' contains {}.", key, items);
    }

    private Set<Long> read() {
        var result = state.getValues(key)
            .map(s -> s.mapToLong(Long::parseLong)
                .toArray())
            .orElse(NO_LONGS);
        LOGGER.trace("'{}' read {}.", key, result);
        return LongStream.of(result)
            .boxed()
            .collect(Collectors.toSet());
    }

    private void write(final Collection<Long> items) {
        if (items.isEmpty()) {
            state.update(c -> c.remove(key));
        } else {
            state.update(b -> b.put(key, items.stream()
                .map(String::valueOf)));
        }
        var value = state.getValue(key)
            .orElse("nothing");
        LOGGER.trace("'{}' wrote '{}'.", key, value);
    }

    /**
     * Immediately writes the item to the underlying storage.
     * 
     * @param item
     */
    public synchronized void put(final T item) {
        if (isEnabled) { // Only do work in dry run.
            items.add(idSupplier.applyAsLong(item));
        }
        write(items); // But store the results anyway, so that the stale dry run data is removed.
    }

    /**
     * Whether or not an item is contained in the underlying storage.
     * 
     * @param item
     */
    public synchronized boolean contains(final T item) {
        return items.contains(idSupplier.applyAsLong(item));
    }

    @Override
    public String toString() {
        return "SessionState{" +
                "items=" + items +
                ", key='" + key + '\'' +
                '}';
    }
}
