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
import java.util.function.Consumer;
import java.util.function.Supplier;
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
public enum State {

    INSTANCE; // cheap thread-safe singleton

    private final Logger LOGGER = LoggerFactory.getLogger(State.class);
    private static final String DELIMITER = ";";
    private static final Pattern SPLIT_BY_DELIMITER = Pattern.compile("\\Q" + DELIMITER + "\\E");
    private final AtomicReference<Ini> stateFile = new AtomicReference<>(null);
    private final Supplier<Ini> stateFileSupplier = () -> {
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
    };

    /**
     * Get state storage for a particular class.
     * @param clz Namespace for the state storage. It is recommended for this to be the calling class.
     * @return State storage unique for the namespace.
     */
    public static State.ClassSpecificState forClass(final Class<?> clz) {
        return new State.ClassSpecificState(clz);
    }

    private Ini getStateFile() {
        return stateFile.updateAndGet(file -> file == null ? stateFileSupplier.get() : file);
    }

    boolean containskey(final String section, final String key) {
        final boolean hasSection = getStateFile().keySet().contains(section);
        return hasSection && getStateFile().get(section).containsKey(key);
    }

    Optional<String> getValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            final String value = getStateFile().get(section, key, String.class);
            if (value.trim().length() == 0) {
                return Optional.empty();
            }
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    Collection<String> getKeys(final String section) {
        if (getStateFile().containsKey(section)) {
            return new HashSet<>(getStateFile().get(section).keySet());
        } else {
            return Collections.emptySet();
        }
    }

    private boolean store() {
        try {
            getStateFile().store();
            this.stateFile.set(null); // reload file when state is next used
            return true;
        } catch (final IOException ex) {
            LOGGER.warn("Failed storing state.", ex);
            return false;
        }
    }

    void unsetValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            getStateFile().get(section).remove(key);
        }
    }

    void setValue(final String section, final String key, final String value) {
        getStateFile().put(section, key, value);
    }

    void unsetValues(final String section) {
        getStateFile().remove(section);
    }

    /**
     * Used for batching write operations to {@link State}. Use {@link #call()} to end the batch and write data to the
     * state file.
     */
    public static class Batch implements Callable<Boolean> {

        private final Collection<Consumer<State.ClassSpecificState>> actions = new ArrayList<>();
        private final State.ClassSpecificState state;

        private Batch(final State.ClassSpecificState state, final boolean fresh) {
            this.state = state;
            if (fresh) {
                actions.add(State.ClassSpecificState::reset);
            }
        }

        public State.Batch set(final String key, final String value) {
            actions.add((state) -> state.setValue(key, value));
            return this;
        }

        public State.Batch set(final String key, final Stream<String> values) {
            return set(key, values.collect(Collectors.joining(DELIMITER)));
        }

        public State.Batch unset(final String key) {
            actions.add((state) -> state.unsetValue(key));
            return this;
        }

        @Override
        public Boolean call() {
            synchronized (State.INSTANCE) {
                actions.forEach(a -> a.accept(state));
                return State.INSTANCE.store();
            }
        }
    }

    /**
     * Isolated key-value map of strings.
     */
    public static class ClassSpecificState {

        private final String classIdentifier;

        ClassSpecificState(final Class<?> clz) {
            this.classIdentifier = clz.getName();
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
            synchronized (State.INSTANCE) {
                return State.INSTANCE.getValue(this.classIdentifier, key);
            }
        }

        public Optional<List<String>> getValues(final String key) {
            return getValue(key).map(value -> Arrays.asList(SPLIT_BY_DELIMITER.split(value)));
        }

        /**
         * Retrieve all keys associated with this class-specific state storage.
         * @return Unique key values.
         */
        public Collection<String> getKeys() {
            synchronized (State.INSTANCE) {
                return State.INSTANCE.getKeys(this.classIdentifier);
            }
        }

        /**
         * Store a value in this class-specific state storage, overwriting any value previously stored under this key.
         * @param key Key to store the value under.
         * @param value The value to store.
         */
        void setValue(final String key, final String value) {
            State.INSTANCE.setValue(this.classIdentifier, key, value);
        }

        /**
         * Remove the value associated with a given key, so that {@link #getValue(String)} will be empty.
         * @param key Key to disassociate.
         */
        void unsetValue(final String key) {
            State.INSTANCE.unsetValue(this.classIdentifier, key);
        }

        /**
         * Remove all values for this class-specific state storage.
         */
        void reset() {
            State.INSTANCE.unsetValues(classIdentifier);
        }
    }

}
