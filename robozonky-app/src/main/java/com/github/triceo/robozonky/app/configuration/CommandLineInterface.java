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

import java.io.File;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Stream;

import com.github.triceo.robozonky.app.App;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import org.apache.commons.cli.CommandLine;
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
public class CommandLineInterface {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandLineInterface.class);

    static final int DEFAULT_CAPTCHA_DELAY_SECONDS = 2 * 60;
    static final int DEFAULT_SLEEP_PERIOD_MINUTES = 60;

    static final Option OPTION_STRATEGY = Option.builder("s").hasArg().longOpt("strategy")
            .argName("Investment strategy").desc("Points to a resource holding the investment strategy configuration.")
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
            .argName("Delay in seconds before new loans lose CAPTCHA protection.").longOpt("closed-season")
            .desc("Allows to override the default CAPTCHA loan delay.").build();
    static final Option OPTION_ZONK = Option.builder("z").hasArg()
            .argName("The longest amount of time in minutes for which Zonky is allowed to sleep.").longOpt("zonk")
            .desc("Allows to override the default length of sleep period.").build();
    static final Option OPTION_CONFIRMATION = Option.builder("x").hasArg().optionalArg(false)
            .argName("'tool:token'").longOpt("external")
            .desc("Use external tool to confirm investments.").build();

    /**
     * Parse the command line.
     *
     * @param args Command line arguments as received by {@link App#main(String...)}.
     * @return Empty if parsing failed, at which point it will write standard help message to sysout.
     */
    public static Optional<CommandLineInterface> parse(final String... args) {
        // create the mode of operation
        final OptionGroup operatingModes = new OptionGroup();
        Stream.of(OperatingMode.values())
                .map(OperatingMode::getSelectingOption)
                .forEach(operatingModes::addOption);
        operatingModes.setRequired(true);
        // include authentication options
        final OptionGroup authenticationModes = new OptionGroup()
                .addOption(CommandLineInterface.OPTION_USERNAME)
                .addOption(CommandLineInterface.OPTION_KEYSTORE);
        authenticationModes.setRequired(true);
        // join all in a single config
        final Options options = new Options()
                .addOptionGroup(operatingModes)
                .addOptionGroup(authenticationModes);
        final Stream<Option> optionsFromOpModes = Stream.of(OperatingMode.values())
                .flatMap(o -> o.getOtherOptions().stream());
        final Stream<Option> otherOptions = Stream.of(
                CommandLineInterface.OPTION_PASSWORD,
                CommandLineInterface.OPTION_USE_TOKEN,
                CommandLineInterface.OPTION_FAULT_TOLERANT
        );
        Stream.concat(optionsFromOpModes, otherOptions).forEach(options::addOption);
        // and initialize
        try {
            final CommandLine cli = new DefaultParser().parse(options, args);
            return Optional.of(new CommandLineInterface(options, cli));
        } catch (final ParseException ex) {
            CommandLineInterface.printHelp(options, ex.getMessage(), true);
            return Optional.empty();
        }
    }

    private final CommandLineWrapper commandLine;
    private final Options options;

    private CommandLineInterface(final Options options, final CommandLine cli) {
        this.options = options;
        this.commandLine = new CommandLineWrapper(cli);
        CommandLineInterface.LOGGER.debug("Received arguments: {}.", this.commandLine);
    }

    public Optional<Configuration> newApplicationConfiguration(final SecretProvider secretProvider) {
        return Stream.of(OperatingMode.values())
                .filter(mode -> this.commandLine.hasOption(mode.getSelectingOption()))
                .map(mode -> mode.configure(this, secretProvider))
                .findFirst()
                .orElse(Optional.empty());
    }

    private AuthenticationHandler createAuthenticationHandler(final SecretProvider secrets) {
        if (this.isTokenEnabled()) {
            final int secs = this.getTokenRefreshBeforeExpirationInSeconds().orElse(60);
            final TemporalAmount duration = Duration.ofSeconds(secs);
            return AuthenticationHandler.tokenBased(secrets, duration);
        } else {
            return AuthenticationHandler.passwordBased(secrets);
        }
    }

    public Optional<AuthenticationHandler> newAuthenticationHandler() {
        return SecretProviderFactory.getSecretProvider(this)
                .map(secrets -> Optional.of(this.createAuthenticationHandler(secrets)))
                .orElse(Optional.empty());
    }

    Optional<String> getStrategyConfigurationLocation() {
        return this.commandLine.getOptionValue(OperatingMode.STRATEGY_DRIVEN.getSelectingOption());
    }

    Optional<String> getUsername() {
        return this.commandLine.getOptionValue(CommandLineInterface.OPTION_USERNAME);
    }

    char[] getPassword() {
        return this.commandLine.getOptionValue(CommandLineInterface.OPTION_PASSWORD).get().toCharArray();
    }

    OptionalInt getLoanId() {
        return this.commandLine.getIntegerOptionValue(CommandLineInterface.OPTION_INVESTMENT);
    }

    OptionalInt getLoanAmount() {
        return this.commandLine.getIntegerOptionValue(CommandLineInterface.OPTION_AMOUNT);
    }

    boolean isDryRun() {
        return this.commandLine.hasOption(CommandLineInterface.OPTION_DRY_RUN);
    }

    boolean isTokenEnabled() {
        return this.commandLine.hasOption(CommandLineInterface.OPTION_USE_TOKEN);
    }

    public boolean isFaultTolerant() {
        return this.commandLine.hasOption(CommandLineInterface.OPTION_FAULT_TOLERANT);
    }

    int getCaptchaPreventingInvestingDelayInSeconds() { // FIXME do not allow negative values
        return this.commandLine.getIntegerOptionValue(CommandLineInterface.OPTION_CLOSED_SEASON,
                CommandLineInterface.DEFAULT_CAPTCHA_DELAY_SECONDS);
    }

    int getMaximumSleepPeriodInMinutes() { // FIXME do not allow negative values
        return this.commandLine.getIntegerOptionValue(CommandLineInterface.OPTION_ZONK,
                CommandLineInterface.DEFAULT_SLEEP_PERIOD_MINUTES);
    }

    OptionalInt getTokenRefreshBeforeExpirationInSeconds() {
        return this.commandLine.getIntegerOptionValue(CommandLineInterface.OPTION_USE_TOKEN);
    }

    Optional<File> getKeyStoreLocation() {
        return this.commandLine.getOptionValue(CommandLineInterface.OPTION_KEYSTORE)
                .map(value -> Optional.of(new File(value)))
                .orElse(Optional.empty());
    }

    OptionalInt getDryRunBalance() {
        return this.commandLine.getIntegerOptionValue(CommandLineInterface.OPTION_DRY_RUN);
    }

    Optional<ConfirmationCredentials> getConfirmationCredentials() {
        return this.commandLine.getOptionValue(CommandLineInterface.OPTION_CONFIRMATION)
                .map(value -> Optional.of(new ConfirmationCredentials(value)))
                .orElse(Optional.empty());
    }

    static String getScriptIdentifier() {
        return System.getProperty("os.name").contains("Windows") ? "robozonky.bat" : "robozonky.sh";
    }

    private static void printHelp(final Options options, final String message, final boolean isError) {
        final String actualMessage = isError ? "Error: " + message : message;
        new HelpFormatter().printHelp(CommandLineInterface.getScriptIdentifier(), null, options, actualMessage, true);
    }

    void printHelp(final String message, final boolean isError) {
        CommandLineInterface.printHelp(this.options, message, isError);
    }

}
