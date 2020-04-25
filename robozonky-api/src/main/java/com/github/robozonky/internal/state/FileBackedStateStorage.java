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

package com.github.robozonky.internal.state;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.json.bind.JsonbConfig;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.Defaults;

class FileBackedStateStorage implements StateStorage {

    private static final Logger LOGGER = LogManager.getLogger(FileBackedStateStorage.class);

    private final File stateLocation;
    private final AtomicReference<Map<String, Map<String, String>>> state = new AtomicReference<>();

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

    private synchronized Map<String, Map<String, String>> getState() {
        if (state.get() == null) {
            if (!stateLocation.exists()) {
                state.set(new ConcurrentHashMap<>(0));
            } else {
                try (Jsonb jsonb = JsonbBuilder.create()) {
                    LOGGER.trace("Reading state: '{}'.", stateLocation);
                    String json = new String(Files.readAllBytes(stateLocation.toPath()));
                    Map<String, Map<String, String>> deserialized = jsonb.fromJson(json, Map.class);
                    state.set(new ConcurrentHashMap<>(deserialized));
                } catch (final Exception ex) {
                    Path oldStateLocation = stateLocation.toPath();
                    Path corruptedStateLocation = Path.of(oldStateLocation.toAbsolutePath() + ".corrupted");
                    try {
                        LOGGER.debug("State file corruption detected.", ex);
                        Files.move(oldStateLocation, corruptedStateLocation);
                        LOGGER.warn("Using clean state, old state moved to {}.", corruptedStateLocation);
                        return getState();
                    } catch (final IOException ex2) {
                        throw new IllegalStateException(
                                "State file corrupted and could not be fixed: " + oldStateLocation,
                                ex2);
                    }
                }
            }
        }
        return state.get();
    }

    private boolean containskey(final String section, final String key) {
        final boolean hasSection = getState().keySet()
            .contains(section);
        return hasSection && getState().get(section)
            .containsKey(key);
    }

    @Override
    public Optional<String> getValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            final String value = getState().get(section)
                .get(key)
                .trim();
            if (value.length() == 0) {
                return Optional.empty();
            }
            return Optional.of(value);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public Stream<String> getKeys(final String section) {
        var internalState = getState();
        if (internalState.containsKey(section)) {
            return internalState.get(section)
                .keySet()
                .stream();
        } else {
            return Stream.empty();
        }
    }

    @Override
    public Stream<String> getSections() {
        return getState().keySet()
            .stream();
    }

    @Override
    public void setValue(final String section, final String key, final String value) {
        LOGGER.trace("Setting '{}' in '{}' to '{}'.", key, section, value);
        getState().computeIfAbsent(section, __ -> new ConcurrentHashMap<>(1))
            .put(key, value);
    }

    @Override
    public void unsetValue(final String section, final String key) {
        if (this.containskey(section, key)) {
            LOGGER.trace("Unsetting '{}' in '{}'.", key, section);
            getState().get(section)
                .remove(key);
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
        JsonbConfig config = new JsonbConfig()
            .withFormatting(true);
        try (Jsonb jsonb = JsonbBuilder.create(config)) {
            String json = jsonb.toJson(this.getState());
            Files.write(stateLocation.toPath(), json.getBytes(Defaults.CHARSET));
            LOGGER.debug("Stored state: '{}'.", stateLocation);
            return true;
        } catch (final Exception e) {
            LOGGER.warn("Failed storing state.", e);
            return false;
        }
    }
}
