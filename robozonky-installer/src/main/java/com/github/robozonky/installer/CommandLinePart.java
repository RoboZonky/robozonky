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

package com.github.robozonky.installer;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.internal.api.Defaults;

public class CommandLinePart {

    private final Map<String, Collection<String>> options = new LinkedHashMap<>(0);
    private final Map<String, String> properties = new LinkedHashMap<>(0);
    private final Map<String, String> environmentVariables = new LinkedHashMap<>(0);
    private final Map<String, Optional<String>> jvmArguments = new LinkedHashMap<>(0);

    public CommandLinePart setOption(final String key, final String... value) {
        this.options.put(key, Collections.unmodifiableCollection(Arrays.asList(value)));
        return this;
    }

    public CommandLinePart setProperty(final String key, final String value) {
        this.properties.put(key, value);
        return this;
    }

    public CommandLinePart setJvmArgument(final String argument) {
        this.jvmArguments.put(argument, Optional.empty());
        return this;
    }

    public CommandLinePart setJvmArgument(final String argument, final String value) {
        this.jvmArguments.put(argument, Optional.of(value));
        return this;
    }

    public CommandLinePart setEnvironmentVariable(final String key, final String value) {
        this.environmentVariables.put(key, value);
        return this;
    }

    public Map<String, Collection<String>> getOptions() {
        return Collections.unmodifiableMap(options);
    }

    public Map<String, String> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public Map<String, String> getEnvironmentVariables() {
        return Collections.unmodifiableMap(environmentVariables);
    }

    private Collection<String> getOptionItems() {
        return options.entrySet().stream()
                .flatMap(e -> {
                    final Stream<String> key = Stream.of(e.getKey());
                    final Stream<String> values = e.getValue().stream().map(v -> "\"" + v + "\"");
                    return Stream.concat(key, values);
                }).collect(Collectors.toList());
    }

    public String convertOptions() {
        return String.join(" ", this.getOptionItems());
    }

    public void storeOptions(final File cliFile) throws IOException {
        Files.write(cliFile.toPath(), this.getOptionItems(), Defaults.CHARSET);
    }

    public Map<String, Optional<String>> getJvmArguments() {
        return Collections.unmodifiableMap(jvmArguments);
    }
}
