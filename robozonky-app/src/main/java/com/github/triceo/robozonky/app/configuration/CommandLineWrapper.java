/*
 * Copyright 2016 Lukáš Petrovický
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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CommandLineWrapper {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineWrapper.class);

    /**
     * Convert the command line arguments into a string and log it.
     * @param cli Parsed command line.
     */
    private static void logOptionValues(final CommandLine cli) {
        final String optionsString = Arrays.stream(cli.getOptions())
                .filter(o -> cli.hasOption(o.getOpt()))
                .filter(o -> !o.equals(CommandLineInterface.OPTION_PASSWORD))
                .map(o -> {
                    String result = "-" + o.getOpt();
                    final String value = cli.getOptionValue(o.getOpt());
                    if (value != null) {
                        result += " " + value;
                    }
                    return result;
                }).collect(Collectors.joining(", ", "[", "]"));
        CommandLineWrapper.LOGGER.debug("Received command line: {}.", optionsString);
    }

    private final CommandLine cli;

    public CommandLineWrapper(final CommandLine cli) {
        this.cli = cli;
        CommandLineWrapper.logOptionValues(cli);
    }

    public boolean hasOption(final Option option) {
        return this.cli.hasOption(option.getOpt());
    }

    public Optional<String> getOptionValue(final Option option) {
        if (this.hasOption(option)) {
            final String val = this.cli.getOptionValue(option.getOpt());
            if (val == null || val.isEmpty()) {
                return Optional.empty();
            } else {
                return Optional.of(val);
            }
        } else {
            return Optional.empty();
        }
    }

    public OptionalInt getIntegerOptionValue(final Option option) {
        final Optional<String> result = this.getOptionValue(option);
        if (result.isPresent()) {
            try {
                return OptionalInt.of(Integer.parseInt(result.get()));
            } catch (final NumberFormatException ex) {
                return OptionalInt.empty();
            }
        } else {
            return OptionalInt.empty();
        }
    }


}
