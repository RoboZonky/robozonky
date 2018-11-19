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

package com.github.robozonky.notifications;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;

public class ConfigStorage {

    private final Properties storage;

    private ConfigStorage(final Properties properties) {
        this.storage = properties;
    }

    public static ConfigStorage create(final File file) throws IOException {
        return create(Files.newInputStream(file.toPath()));
    }

    public static ConfigStorage create(final InputStream inputStream) throws IOException {
        final Properties props = new Properties();
        props.load(inputStream);
        return new ConfigStorage(props);
    }

    public Optional<String> read(final Target target, final String key) {
        final String prefix = target.getId();
        final String mostSpecific = prefix + "." + key;
        if (storage.containsKey(mostSpecific)) {
            return Optional.of(storage.getProperty(mostSpecific));
        } else {
            return Optional.ofNullable(storage.getProperty(key));
        }
    }

    public String read(final Target target, final String key, final String defaultValue) {
        return read(target, key).orElse(defaultValue);
    }

    public boolean readBoolean(final Target target, final String propertyName, final boolean defaultValue) {
        return readBoolean(target, propertyName).orElse(defaultValue);
    }

    public Optional<Boolean> readBoolean(final Target target, final String propertyName) {
        return read(target, propertyName).map(Boolean::valueOf);
    }

    public OptionalInt readInt(final Target target, final String propertyName) {
        return read(target, propertyName)
                .map(v -> OptionalInt.of(Integer.parseInt(v)))
                .orElse(OptionalInt.empty());
    }

    public int readInt(final Target target, final String propertyName, final int defaultValue) {
        return this.readInt(target, propertyName).orElse(defaultValue);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final ConfigStorage that = (ConfigStorage) o;
        return Objects.equals(storage, that.storage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(storage);
    }
}
