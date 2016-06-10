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

import java.io.File;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class CommandLineInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineInterface.class);

    static final Option OPTION_STRATEGY = Option.builder("s").hasArg().longOpt("strategy")
            .argName("Investment strategy").desc("Points to a file that holds the investment strategy configuration.")
            .build();
    static final Option OPTION_INVESTMENT = Option.builder("l").hasArg().longOpt("loan")
            .argName("Single loan ID").desc("Ignore strategy, invest to one specific loan and exit.")
            .build();
    static final Option OPTION_AMOUNT = Option.builder("a").hasArg().longOpt("amount")
            .argName("Amount to invest").desc("Amount to invest to a single loan when ignoring strategy.")
            .build();
    static final Option OPTION_USERNAME = Option.builder("u").hasArg().longOpt("username")
            .argName("Zonky username").desc("Used to connect to the Zonky server.").build();
    static final Option OPTION_KEYSTORE = Option.builder("g").hasArg().longOpt("guarded")
            .argName("Guarded file").desc("Path to secure file that contains username, password etc.").build();
    static final Option OPTION_PASSWORD = Option.builder("p").required().hasArg().longOpt("password")
            .argName("Guarded password").desc("Used to connect to the Zonky server, or to read the secure storage.")
            .build();
    static final Option OPTION_USE_TOKEN = Option.builder("r").hasArg().optionalArg(true)
            .argName("Seconds before expiration").longOpt("refresh")
            .desc("Once logged in, RoboZonky will never log out unless login expires. Use with caution.").build();
    static final Option OPTION_DRY_RUN = Option.builder("d").hasArg().optionalArg(true).
            argName("Dry run balance").longOpt("dry").desc("Simulate the investments, but never actually spend money.")
            .build();

    public static CommandLineInterface parse(final String... args) {
        // create the mode of operation
        final OptionGroup operatingModes = new OptionGroup();
        operatingModes.setRequired(true);
        Stream.of(OperatingMode.values()).forEach(mode -> operatingModes.addOption(mode.getSelectingOption()));
        // find all options from all modes of operation
        final Collection<Option> ops = Stream.of(OperatingMode.values()).map(OperatingMode::getOtherOptions)
                .collect(LinkedHashSet::new, LinkedHashSet::addAll, LinkedHashSet::addAll);
        // include authentication options
        final OptionGroup authenticationModes = new OptionGroup();
        authenticationModes.setRequired(true);
        authenticationModes.addOption(CommandLineInterface.OPTION_USERNAME);
        authenticationModes.addOption(CommandLineInterface.OPTION_KEYSTORE);
        ops.add(CommandLineInterface.OPTION_PASSWORD);
        ops.add(CommandLineInterface.OPTION_USE_TOKEN);
        // join all in a single config
        final Options options = new Options();
        options.addOptionGroup(operatingModes);
        options.addOptionGroup(authenticationModes);
        ops.forEach(options::addOption);
        final CommandLineParser parser = new DefaultParser();
        // and parse
        try {
            return new CommandLineInterface(options, parser.parse(options, args));
        } catch (final ParseException ex) {
            CommandLineInterface.printHelpAndExit(options, ex.getMessage(), true);
            return null;
        }
    }

    private final CommandLine cli;
    private final Options options;

    private CommandLineInterface(final Options options, final CommandLine cli) {
        this.options = options;
        this.cli = cli;
    }

    Optional<OperatingMode> getCliOperatingMode() {
        for (final OperatingMode mode: OperatingMode.values()) {
            if (cli.hasOption(mode.getSelectingOption().getOpt())) {
                return Optional.of(mode);
            }
        }
        return Optional.empty();
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

    Optional<String> getStrategyConfigurationFilePath() {
        return this.getOptionValue(OperatingMode.STRATEGY_DRIVEN.getSelectingOption());
    }

    public Optional<String> getUsername() {
        return this.getOptionValue(CommandLineInterface.OPTION_USERNAME);
    }

    public String getPassword() {
        return this.getOptionValue(CommandLineInterface.OPTION_PASSWORD).get();
    }

    Optional<Integer> getLoanId() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_INVESTMENT);
    }

    Optional<Integer> getLoanAmount() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_AMOUNT);
    }

    boolean isDryRun() {
        return this.cli.hasOption(CommandLineInterface.OPTION_DRY_RUN.getOpt());
    }

    public boolean isTokenEnabled() {
        return this.cli.hasOption(CommandLineInterface.OPTION_USE_TOKEN.getOpt());
    }

    public Optional<Integer> getTokenRefreshBeforeExpirationInSeconds() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_USE_TOKEN);
    }

    public Optional<File> getKeyStoreLocation() {
        final Optional<String> value = this.getOptionValue(CommandLineInterface.OPTION_KEYSTORE);
        if (value.isPresent()) {
            return Optional.of(new File(value.get()));
        } else {
            return Optional.empty();
        }
    }

    Optional<Integer> getDryRunBalance() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_DRY_RUN);
    }

    static void printHelpAndExit(final Options options, final String message, final boolean isError) {
        final HelpFormatter formatter = new HelpFormatter();
        final String scriptName = System.getProperty("os.name").contains("Windows") ? "robozonky.bat" : "robozonky.sh";
        formatter.printHelp(scriptName, null, options, isError ? "Error: " + message : message, true);
        App.exit(isError ? ReturnCode.ERROR_WRONG_PARAMETERS : ReturnCode.OK);
    }

    public void printHelpAndExit(final String message, final boolean isError) {
        CommandLineInterface.printHelpAndExit(this.options, message, isError);
    }

    public void printHelpAndExit(final String message, final Exception ex) {
        CommandLineInterface.LOGGER.error("Encountered critical error ('{}'), application will exit.", message, ex);
        CommandLineInterface.printHelpAndExit(this.options, message, true);
    }

}
