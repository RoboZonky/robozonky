/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.internal.api;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sometimes some part of the app needs to write out a variable somewhere, to store as state for the next time the
 * app is run. This class can be used for that. It is backed by a single file, all write operations are batched via
 * {@link State.Batch} and only stored at the end.
 * <p>
 * Using this class, only store transient information that, if deleted by a system restart or a similar occasion, would
 * not cause a crash of the application.
 */
public class State {

    private static final Logger LOGGER = LoggerFactory.getLogger(State.class);
    private static final String DELIMITER = ";";
    private static final Pattern SPLIT_BY_DELIMITER = Pattern.compile("\\Q" + DELIMITER + "\\E");
    private final Ini stateFile = getStateFile();

    private State() {
        // no external instances
    }

    /**
     * Get state storage for a particular class.
     * @param clz Namespace for the state storage. It is recommended for this to be the calling class.
     * @return State storage unique for the namespace.
     */
    public static State.ClassSpecificState forClass(final Class<?> clz) {
        return new State.ClassSpecificState(clz);
    }

    private Ini getStateFile() {
        try {
            final File stateLocation = Settings.INSTANCE.getStateFile();
            if (!stateLocation.exists()) {
                LOGGER.debug("Creating state: {}.", stateLocation);
                stateLocation.createNewFile();
            }
            LOGGER.debug("Reading state: {}.", stateLocation);
            return new Ini(stateLocation);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed initializing state.", ex);
        }
    }

    boolean containskey(final String section, final String key) {
        final boolean hasSection = stateFile.keySet().contains(section);
        return hasSection && stateFile.get(section).containsKey(key);
    }

    Optional<String> getValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            final String value = stateFile.get(section, key, String.class);
            if (value.trim().length() == 0) {
                return Optional.empty();
            }
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    Collection<String> getKeys(final String section) {
        if (stateFile.containsKey(section)) {
            return new HashSet<>(stateFile.get(section).keySet());
        } else {
            return Collections.emptySet();
        }
    }

    private boolean store() {
        try {
            stateFile.store();
            return true;
        } catch (final IOException ex) {
            LOGGER.warn("Failed storing state.", ex);
            return false;
        }
    }

    void unsetValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            stateFile.get(section).remove(key);
        }
    }

    void setValue(final String section, final String key, final String value) {
        stateFile.put(section, key, value);
    }

    void unsetValues(final String section) {
        stateFile.remove(section);
    }

    /**
     * Used for batching write operations to {@link State}. Use {@link #call()} to end the batch and write data to the
     * state file.
     */
    public static class Batch implements Callable<Boolean> {

        private final Collection<BiConsumer<State, State.ClassSpecificState>> actions = new ArrayList<>(0);
        private final State.ClassSpecificState state;

        private Batch(final State.ClassSpecificState state, final boolean fresh) {
            this.state = state;
            if (fresh) { // first action is to reset the class-specific state
                actions.add((internalState, classSpecificState) ->
                                    internalState.unsetValues(classSpecificState.classIdentifier));
            }
        }

        public State.Batch set(final String key, final String value) {
            actions.add((state, classSpecificState) -> classSpecificState.setValue(state, key, value));
            return this;
        }

        public State.Batch set(final String key, final Stream<String> values) {
            return set(key, values.collect(Collectors.joining(DELIMITER)));
        }

        public State.Batch unset(final String key) {
            actions.add((state, classSpecificState) -> classSpecificState.unsetValue(state, key));
            return this;
        }

        @Override
        public Boolean call() {
            final State internal = new State();
            actions.forEach(a -> a.accept(internal, state));
            final boolean result = internal.store();
            state.refresh();
            return result;
        }
    }

    /**
     * Isolated key-value map of strings.
     */
    public static class ClassSpecificState {

        private final String classIdentifier;
        private final AtomicReference<State> cache = new AtomicReference<>();

        ClassSpecificState(final Class<?> clz) {
            this.classIdentifier = clz.getName();
            this.refresh();
        }

        void refresh() {
            cache.set(new State());
        }

        public State.Batch newBatch() {
            return newBatch(false);
        }

        public State.Batch newBatch(final boolean resetBeforeStarting) {
            return new State.Batch(this, resetBeforeStarting);
        }

        /**
         * Retrieve a value from this class-specific state storage.
         * @param key Key under which the value was previously stored.
         * @return Present if the storage contains a value for the key.
         */
        public Optional<String> getValue(final String key) {
            return cache.get().getValue(this.classIdentifier, key);
        }

        public Optional<List<String>> getValues(final String key) {
            return getValue(key).map(value -> Arrays.asList(SPLIT_BY_DELIMITER.split(value)));
        }

        /**
         * Retrieve all keys associated with this class-specific state storage.
         * @return Unique key values.
         */
        public Collection<String> getKeys() {
            return cache.get().getKeys(this.classIdentifier);
        }

        /**
         * Store a value in this class-specific state storage, overwriting any value previously stored under this key.
         * @param key Key to store the value under.
         * @param value The value to store.
         */
        void setValue(final State state, final String key, final String value) {
            state.setValue(this.classIdentifier, key, value);
        }

        /**
         * Remove the value associated with a given key, so that {@link #getValue(String)} will be empty.
         * @param key Key to disassociate.
         */
        void unsetValue(final State state, final String key) {
            state.unsetValue(this.classIdentifier, key);
        }

        /**
         * Remove all values for this class-specific state storage.
         */
        public void reset() {
            newBatch(true).call();
        }
    }
}
