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

package com.github.triceo.robozonky.app.configuration;

import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;

class CommandLineWrapper {

    private final CommandLine cli;

    public CommandLineWrapper(final CommandLine cli) {
        this.cli = cli;
    }

    public boolean hasOption(final Option option) {
        return this.cli.hasOption(option.getOpt());
    }

    public Optional<String> getOptionValue(final Option option) {
        if (!this.hasOption(option)) {
            return Optional.empty();
        }
        final String val = this.cli.getOptionValue(option.getOpt());
        if (val == null || val.isEmpty()) {
            return Optional.empty();
        } else {
            return Optional.of(val);
        }
    }

    public OptionalInt getIntegerOptionValue(final Option option) {
        return this.getOptionValue(option)
                .map(value -> {
                    try {
                        return OptionalInt.of(Integer.parseInt(value));
                    } catch (final NumberFormatException ex) {
                        return OptionalInt.empty();
                    }
                }).orElse(OptionalInt.empty());
    }

    public int getIntegerOptionValue(final Option option, final int defaultValue) {
        return this.getIntegerOptionValue(option).orElse(defaultValue);
    }

    @Override
    public String toString() {
        return Arrays.stream(this.cli.getOptions())
                .filter(o -> !Objects.equals(o, CommandLineInterface.OPTION_PASSWORD))
                .map(Option::getOpt)
                .filter(cli::hasOption)
                .map(opt -> {
                    String result = "-" + opt;
                    final String value = cli.getOptionValue(opt);
                    if (value != null) {
                        result += " " + value;
                    }
                    return result;
                }).collect(Collectors.joining(", ", "[", "]"));
    }

}
