/*
 * Copyright 2019 The RoboZonky Project
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

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.ini4j.Ini;

class FileBackedStateStorage implements StateStorage {

    private static final Logger LOGGER = LogManager.getLogger(FileBackedStateStorage.class);

    private final File stateLocation;
    private final AtomicReference<Ini> state = new AtomicReference<>();

    public FileBackedStateStorage(final File file) {
        this.stateLocation = file;
    }

    synchronized void destroy() {
        try {
            Files.deleteIfExists(stateLocation.toPath());
        } catch (final IOException ex) {
            LOGGER.debug("Failed deleting state file.", ex);
        } finally {
            state.set(null);
        }
    }

    private synchronized Ini getState() {
        if (state.get() == null) {
            try {
                if (!stateLocation.exists()) {
                    final boolean created = stateLocation.createNewFile();
                    LOGGER.debug("Created state file '{}': {}.", stateLocation, created);
                }
                LOGGER.trace("Reading state: '{}'.", stateLocation);
                state.set(new Ini(stateLocation));
            } catch (final IOException ex) {
                throw new IllegalStateException("Failed initializing state.", ex);
            }
        }
        return state.get();
    }

    private boolean containskey(final String section, final String key) {
        final boolean hasSection = getState().keySet().contains(section);
        return hasSection && getState().get(section).containsKey(key);
    }

    @Override
    public Optional<String> getValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            final String value = getState().get(section, key, String.class);
            if (value.trim().length() == 0) {
                return Optional.empty();
            }
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Stream<String> getKeys(final String section) {
        final Ini internalState = getState();
        if (internalState.containsKey(section)) {
            return internalState.get(section).keySet().stream();
        } else {
            return Stream.empty();
        }
    }

    @Override
    public Stream<String> getSections() {
        return getState().keySet().stream();
    }

    @Override
    public void setValue(final String section, final String key, final String value) {
        LOGGER.trace("Setting '{}' in '{}' to '{}'.", key, section, value);
        getState().put(section, key, value);
    }

    @Override
    public void unsetValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            LOGGER.trace("Unsetting '{}' in '{}'.", key, section);
            getState().get(section).remove(key);
        } else {
            LOGGER.trace("Unsetting non-existent '{}' in '{}'.", key, section);
        }
    }

    @Override
    public void unsetValues(final String section) {
        LOGGER.trace("Unsetting values in '{}'.", section);
        getState().remove(section);
    }

    @Override
    public synchronized boolean store() {
        try {
            this.getState().store(stateLocation);
            LOGGER.debug("Stored state: '{}'.", stateLocation);
            return true;
        } catch (final IOException e) {
            LOGGER.warn("Failed storing state.", e);
            return false;
        }
    }
}
