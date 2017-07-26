/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.internal.api;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;

import org.ini4j.Ini;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Sometimes some part of the app needs to write out a variable somewhere, to store as state for the next time the
 * app is run. This class can be used for that. It is backed by a single file, all write operations are immediately
 * stored.
 * <p>
 * Using this class, only store transient information that, if deleted by a system restart or a similar occasion, would
 * not cause a crash of the application.
 */
public enum State {

    INSTANCE;

    /**
     * Isolated key-value map of strings.
     */
    public static class ClassSpecificState {

        private final String classIdentifier;

        ClassSpecificState(final Class<?> clz) {
            this.classIdentifier = clz.getName();
        }

        /**
         * Retrieve a value from this class-specific state storage.
         * @param key Key under which the value was previously stored.
         * @return Present if the storage contains a value for the key.
         */
        public Optional<String> getValue(final String key) {
            return State.INSTANCE.getValue(this.classIdentifier, key);
        }

        /**
         * Retrieve all keys associated with this class-specific state storage.
         * @return Unique key values.
         */
        public Collection<String> getKeys() {
            return State.INSTANCE.getKeys(this.classIdentifier);
        }

        /**
         * Store a value in this class-specific state storage, overwriting any value previously stored under this key.
         * @param key Key to store the value under.
         * @param value The value to store.
         */
        public boolean setValue(final String key, final String value) {
            return State.INSTANCE.setValue(this.classIdentifier, key, value);
        }

        /**
         * Remove the value associated with a given key, so that {@link #getValue(String)} will be empty.
         * @param key Key to disassociate.
         * @return True if there previously was a value associated with the key.
         */
        public boolean unsetValue(final String key) {
            return State.INSTANCE.unsetValue(this.classIdentifier, key);
        }

        /**
         * Remove all values for this class-specific state storage.
         * @return True if removed.
         */
        public boolean reset() {
            return State.INSTANCE.unsetValues(classIdentifier);
        }
    }

    static File getStateLocation() {
        return new File(System.getProperty("user.dir"), "robozonky.state");
    }

    private final Logger LOGGER = LoggerFactory.getLogger(State.class);
    private final Ini stateFile;

    State() {
        try {
            // FIXME when the state file is deleted while the JVM is running, the API does not reflect it
            final File stateLocation = State.getStateLocation();
            if (!stateLocation.exists()) {
                LOGGER.debug("Creating state: {}.", stateLocation);
                stateLocation.createNewFile();
            }
            LOGGER.debug("Reading state: {}.", stateLocation);
            this.stateFile = new Ini(stateLocation);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed initializing state.", ex);
        }
    }

    /**
     * Get state storage for a particular class.
     * @param clz Namespace for the state storage. It is recommended for this to be the calling class.
     * @return State storage unique for the namespace.
     */
    public synchronized State.ClassSpecificState forClass(final Class<?> clz) {
        return new State.ClassSpecificState(clz);
    }

    synchronized boolean containskey(final String section, final String key) {
        final boolean hasSection = this.stateFile.keySet().contains(section);
        return hasSection && this.stateFile.get(section).containsKey(key);
    }

    synchronized Optional<String> getValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            return Optional.of(this.stateFile.get(section, key, String.class));
        } else {
            return Optional.empty();
        }
    }

    synchronized Collection<String> getKeys(final String section) {
        if (this.stateFile.containsKey(section)) {
            return new HashSet<>(this.stateFile.get(section).keySet());
        } else {
            return Collections.emptySet();
        }
    }

    private synchronized boolean store() {
        try {
            this.stateFile.store();
            return true;
        } catch (final IOException ex) {
            LOGGER.warn("Failed storing state.", ex);
            return false;
        }
    }

    synchronized boolean unsetValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            return (this.stateFile.get(section).remove(key) != null) && this.store();
        } else {
            return false;
        }
    }

    synchronized boolean setValue(final String section, final String key, final String value) {
        this.stateFile.put(section, key, value);
        return this.store();
    }

    synchronized boolean unsetValues(final String section) {
        return this.stateFile.remove(section) != null && this.store();
    }

}
