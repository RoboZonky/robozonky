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

package com.github.triceo.robozonky.app;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionGroup;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

class CommandLineInterface {

    static final Option OPTION_STRATEGY = Option.builder("s").hasArg().longOpt("strategy")
            .argName("Investment strategy").desc("Points to a file that holds the investment strategy configuration.")
            .build();
    static final Option OPTION_INVESTMENT = Option.builder("l").hasArg().longOpt("loan")
            .argName("Single loan ID").desc("Ignore strategy, invest to one specific loan and exit.")
            .build();
    static final Option OPTION_AMOUNT = Option.builder("a").hasArg().longOpt("amount")
            .argName("Amount to invest").desc("Amount to invest to a single loan when ignoring strategy.")
            .build();
    static final Option OPTION_HELP = Option.builder("h").longOpt("help").desc("Show this help message and quit.")
            .build();
    static final Option OPTION_USERNAME = Option.builder("u").hasArg().longOpt("username")
            .argName("Zonky username").desc("Used to connect to the Zonky server.").build();
    static final Option OPTION_PASSWORD = Option.builder("p").hasArg().longOpt("password").argName("Zonky password").desc("Used to connect to the Zonky server.").build();
    static final Option OPTION_USE_TOKEN = Option.builder("r").hasArg().optionalArg(true)
            .argName("Seconds before expiration").longOpt("refresh")
            .desc("Once logged in, RoboZonky will never log out unless login expires. Use with caution.").build();
    static final Option OPTION_DRY_RUN = Option.builder("d").hasArg().optionalArg(true).
            argName("Dry run balance").longOpt("dry").desc("Simulate the investments, but never actually spend money.")
            .build();

    public static CommandLineInterface parse(final String... args) {
        // create the mode of operation
        final OptionGroup og = new OptionGroup();
        og.setRequired(true);
        Stream.of(OperatingMode.values()).forEach(mode -> og.addOption(mode.getSelectingOption()));
        // find all options from all modes of operation
        final Collection<Option> ops = Stream.of(OperatingMode.values()).map(OperatingMode::getOtherOptions)
                .collect(LinkedHashSet::new, LinkedHashSet::addAll, LinkedHashSet::addAll);
        // include authentication options
        ops.add(CommandLineInterface.OPTION_USERNAME);
        ops.add(CommandLineInterface.OPTION_PASSWORD);
        ops.add(CommandLineInterface.OPTION_USE_TOKEN);
        // join all in a single config
        final Options options = new Options();
        options.addOptionGroup(og);
        ops.forEach(options::addOption);
        final CommandLineParser parser = new DefaultParser();
        // and parse
        try {
            return new CommandLineInterface(options, parser.parse(options, args));
        } catch (final ParseException ex) {
            return new CommandLineInterface(options, null);
        }
    }

    private final CommandLine cli;
    private final Options options;

    private CommandLineInterface(final Options options, final CommandLine cli) {
        this.options = options;
        this.cli = cli;
    }

    public OperatingMode getCliOperatingMode() {
        if (this.cli == null) {
            return OperatingMode.HELP;
        }
        for (final OperatingMode mode: OperatingMode.values()) {
            if (cli.hasOption(mode.getSelectingOption().getOpt())) {
                return mode;
            }
        }
        return OperatingMode.HELP;
    }

    private Optional<String> getOptionValue(final Option option) {
        if (this.cli.hasOption(option.getOpt())) {
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

    private Optional<Integer> getIntegerOptionValue(final Option option) {
        final Optional<String> result = this.getOptionValue(option);
        if (result.isPresent()) {
            try {
                return Optional.of(Integer.valueOf(result.get()));
            } catch (final NumberFormatException ex) {
                return Optional.empty();
            }
        } else {
            return Optional.empty();
        }
    }

    public Optional<String> getStrategyConfigurationFilePath() {
        return this.getOptionValue(OperatingMode.STRATEGY_DRIVEN.getSelectingOption());
    }

    public Optional<String> getUsername() {
        return this.getOptionValue(CommandLineInterface.OPTION_USERNAME);
    }

    public Optional<String> getPassword() {
        return this.getOptionValue(CommandLineInterface.OPTION_PASSWORD);
    }

    public Optional<Integer> getLoanId() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_INVESTMENT);
    }

    public Optional<Integer> getLoanAmount() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_AMOUNT);
    }

    public boolean isDryRun() {
        return this.cli.hasOption(CommandLineInterface.OPTION_DRY_RUN.getOpt());
    }

    public boolean isTokenEnabled() {
        return this.cli.hasOption(CommandLineInterface.OPTION_USE_TOKEN.getOpt());
    }

    public Optional<Integer> getTokenRefreshBeforeExpirationInSeconds() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_USE_TOKEN);
    }

    public Optional<Integer> getDryRunBalance() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_DRY_RUN);
    }

    public void printHelpAndExit(final String message, final boolean isError) {
        final HelpFormatter formatter = new HelpFormatter();
        final String scriptName = System.getProperty("os.name").contains("Windows") ? "robozonky.bat" : "robozonky.sh";
        formatter.printHelp(scriptName, null, this.options, isError ? "Error: " + message : message, true);
    }


}
