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
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
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

/**
 * Processes command line arguments and provides access to their values.
 */
class   CommandLineInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineInterface.class);
    private static final int DEFAULT_CAPTCHA_DELAY_SECONDS = 2 * 60;
    private static final int DEFAULT_SLEEP_PERIOD_MINUTES = 60;

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
    static final Option OPTION_FAULT_TOLERANT = Option.builder("t").longOpt("fault-tolerant")
            .desc("RoboZonky will not report errors if it thinks Zonky is down. Use with caution.").build();
    static final Option OPTION_DRY_RUN = Option.builder("d").hasArg().optionalArg(true).
            argName("Dry run balance").longOpt("dry").desc("Simulate the investments, but never actually spend money.")
            .build();
    static final Option OPTION_CLOSED_SEASON = Option.builder("c").hasArg()
            .argName("Delay in seconds before new loans are made available for investing.").longOpt("closed-season")
            .desc("Allows to override the default CAPTCHA loan delay.").build();
    static final Option OPTION_ZONK = Option.builder("z").hasArg()
            .argName("The longest amount of time in minutes for which Zonky is allowed to sleep.").longOpt("zonk")
            .desc("Allows to override the default length of sleep period.").build();

    /**
     * Convert the command line arguments into a string and log it.
     * @param cli Parsed command line.
     */
    private static void logOptionValues(final CommandLine cli) {
        final List<String> optionsString = Arrays.stream(cli.getOptions())
                .filter(o -> cli.hasOption(o.getOpt()))
                .filter(o -> !o.equals(CommandLineInterface.OPTION_PASSWORD))
                .map(o -> {
                    String result = "-" + o.getOpt();
                    final String value = cli.getOptionValue(o.getOpt());
                    if (value != null) {
                        result += " " + value;
                    }
                    return result;
                }).collect(Collectors.toList());
        CommandLineInterface.LOGGER.debug("Processing command line: {}.", optionsString);
    }

    /**
     * Parse the command line.
     *
     * @param args Command line arguments as received by {@link App#main(String...)}.
     * @return Empty if parsing failed, at which point it will write standard help message to sysout.
     */
    public static Optional<CommandLineInterface> parse(final String... args) {
        // create the mode of operation
        final OptionGroup operatingModes = new OptionGroup();
        operatingModes.setRequired(true);
        Stream.of(OperatingMode.values()).forEach(mode -> operatingModes.addOption(mode.getSelectingOption()));
        // include authentication options
        final OptionGroup authenticationModes = new OptionGroup();
        authenticationModes.setRequired(true);
        authenticationModes.addOption(CommandLineInterface.OPTION_USERNAME);
        authenticationModes.addOption(CommandLineInterface.OPTION_KEYSTORE);
        // find all options from all modes of operation
        final Collection<Option> ops = Stream.of(OperatingMode.values()).map(OperatingMode::getOtherOptions)
                .collect(LinkedHashSet::new, LinkedHashSet::addAll, LinkedHashSet::addAll);
        ops.add(CommandLineInterface.OPTION_PASSWORD);
        ops.add(CommandLineInterface.OPTION_USE_TOKEN);
        ops.add(CommandLineInterface.OPTION_FAULT_TOLERANT);
        ops.add(CommandLineInterface.OPTION_CLOSED_SEASON);
        ops.add(CommandLineInterface.OPTION_ZONK);
        // join all in a single config
        final Options options = new Options();
        options.addOptionGroup(operatingModes);
        options.addOptionGroup(authenticationModes);
        ops.forEach(options::addOption);
        final CommandLineParser parser = new DefaultParser();
        // and parse
        try {
            final CommandLine cli = parser.parse(options, args);
            CommandLineInterface.logOptionValues(cli);
            return Optional.of(new CommandLineInterface(options, cli));
        } catch (final ParseException ex) {
            CommandLineInterface.printHelp(options, ex.getMessage(), true);
            return Optional.empty();
        }
    }

    private final CommandLine cli;
    private final Options options;

    private CommandLineInterface(final Options options, final CommandLine cli) {
        this.options = options;
        this.cli = cli;
    }

    OperatingMode getCliOperatingMode() {
        return Stream.of(OperatingMode.values())
                .filter(mode -> this.hasOption(mode.getSelectingOption()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("This situation is impossible."));
    }

    private boolean hasOption(final Option option) {
        return this.cli.hasOption(option.getOpt());
    }

    private Optional<String> getOptionValue(final Option option) {
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

    public char[] getPassword() {
        return this.getOptionValue(CommandLineInterface.OPTION_PASSWORD).get().toCharArray();
    }

    Optional<Integer> getLoanId() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_INVESTMENT);
    }

    Optional<Integer> getLoanAmount() {
        return this.getIntegerOptionValue(CommandLineInterface.OPTION_AMOUNT);
    }

    boolean isDryRun() {
        return this.hasOption(CommandLineInterface.OPTION_DRY_RUN);
    }

    public boolean isTokenEnabled() {
        return this.hasOption(CommandLineInterface.OPTION_USE_TOKEN);
    }

    public boolean isFaultTolerant() {
        return this.hasOption(CommandLineInterface.OPTION_FAULT_TOLERANT);
    }

    public int getCaptchaPreventingInvestingDelayInSeconds() { // FIXME do not allow negaive values
        return this.hasOption(CommandLineInterface.OPTION_CLOSED_SEASON) ?
                this.getIntegerOptionValue(CommandLineInterface.OPTION_CLOSED_SEASON)
                        .orElseThrow(() -> new IllegalStateException("Missing mandatory argument value.")) :
                CommandLineInterface.DEFAULT_CAPTCHA_DELAY_SECONDS;
    }

    public int getMaximumSleepPeriodInMinutes() { // FIXME do not allow negaive values
        return this.hasOption(CommandLineInterface.OPTION_ZONK) ?
                this.getIntegerOptionValue(CommandLineInterface.OPTION_ZONK)
                        .orElseThrow(() -> new IllegalStateException("Missing mandatory argument value.")) :
                CommandLineInterface.DEFAULT_SLEEP_PERIOD_MINUTES;
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

    static String getScriptIdentifier() {
        return System.getProperty("os.name").contains("Windows") ? "robozonky.bat" : "robozonky.sh";
    }

    private static void printHelp(final Options options, final String message, final boolean isError) {
        new HelpFormatter().printHelp(CommandLineInterface.getScriptIdentifier(), null, options,
                isError ? "Error: " + message : message, true);
    }

    public void printHelp(final String message, final boolean isError) {
        CommandLineInterface.printHelp(this.options, message, isError);
    }

}
