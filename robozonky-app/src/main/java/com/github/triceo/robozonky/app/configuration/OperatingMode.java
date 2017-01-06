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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalInt;

import com.github.triceo.robozonky.ZonkyProxy;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import org.apache.commons.cli.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents different modes of operation of the application, their means of selection and configure.
 */
enum OperatingMode {

    /**
     * Requires a strategy and performs 0 or more investments based on the strategy.
     */
    STRATEGY_DRIVEN(CommandLineInterface.OPTION_STRATEGY, CommandLineInterface.OPTION_DRY_RUN,
            CommandLineInterface.OPTION_CONFIRMATION, CommandLineInterface.OPTION_ZONK,
            CommandLineInterface.OPTION_CLOSED_SEASON) {

        @Override
        public Optional<Configuration> configure(final CommandLineInterface cli, final SecretProvider secrets) {
            if (cli.getLoanAmount().isPresent() || cli.getLoanId().isPresent()) {
                cli.printHelp("Loan data makes no sense in this context.", true);
                return Optional.empty();
            }
            final Optional<String> strategyLocation = cli.getStrategyConfigurationLocation();
            if (!strategyLocation.isPresent()) {
                cli.printHelp("Strategy file must be provided.", true);
                return Optional.empty();
            }
            // find investment strategy
            final Optional<InvestmentStrategy> strategy = InvestmentStrategyLoader.load(strategyLocation.get());
            if (!strategy.isPresent()) {
                OperatingMode.LOGGER.error("No investment strategy found to support {}.", strategyLocation);
                return Optional.empty();
            }
            OperatingMode.LOGGER.debug("Strategy '{}' will be processed using '{}'.", strategyLocation.get(),
                    strategy.get().getClass());
            // find confirmation provider and finally create configuration
            final Optional<ZonkyProxy.Builder> optionalBuilder = cli.getConfirmationCredentials()
                    .map(credentials -> OperatingMode.getZonkyProxyBuilder(credentials, secrets))
                    .orElse(Optional.of(new ZonkyProxy.Builder()));
            return optionalBuilder.map(builder -> {
                if (cli.isDryRun()) {
                    final int balance = cli.getDryRunBalance().orElse(-1);
                    return Optional.of(new Configuration(strategy.get(), builder,
                            cli.getMaximumSleepPeriodInMinutes(),
                            cli.getCaptchaPreventingInvestingDelayInSeconds(), balance));
                } else {
                    return Optional.of(new Configuration(strategy.get(), builder,
                            cli.getMaximumSleepPeriodInMinutes(),
                            cli.getCaptchaPreventingInvestingDelayInSeconds()));
                    }
                }).orElse(Optional.empty());
        }
    },
    /**
     * Requires a loan ID, into which it will invest a given amount and terminate.
     */
    USER_DRIVEN(CommandLineInterface.OPTION_INVESTMENT, CommandLineInterface.OPTION_AMOUNT,
            CommandLineInterface.OPTION_DRY_RUN) {

        @Override
        public Optional<Configuration> configure(final CommandLineInterface cli, final SecretProvider secrets) {
            final OptionalInt loanId = cli.getLoanId();
            final OptionalInt loanAmount = cli.getLoanAmount();
            if (!loanId.isPresent() || loanId.getAsInt() < 1) {
                cli.printHelp("Loan ID must be provided and greater than 0.", true);
                return Optional.empty();
            } else if (!loanAmount.isPresent() || loanAmount.getAsInt() < 1) {
                cli.printHelp("Loan amount must be provided and greater than 0.", true);
                return Optional.empty();
            } else if (cli.getConfirmationCredentials().isPresent()) {
                cli.printHelp("External credentials make no sense in manual mode.", true);
                return Optional.empty();
            } else if (cli.isDryRun()) {
                final int balance = cli.getDryRunBalance().orElse(-1);
                return Optional.of(new Configuration(loanId.getAsInt(), loanAmount.getAsInt(),
                        cli.getMaximumSleepPeriodInMinutes(), cli.getCaptchaPreventingInvestingDelayInSeconds(),
                        balance));
            } else {
                return Optional.of(new Configuration(loanId.getAsInt(), loanAmount.getAsInt(),
                        cli.getMaximumSleepPeriodInMinutes(), cli.getCaptchaPreventingInvestingDelayInSeconds()));
            }
        }
    };

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatingMode.class);

    private static Optional<ZonkyProxy.Builder> getZonkyProxyBuilder(final ConfirmationCredentials credentials,
                                                                     final SecretProvider secrets,
                                                                     final ConfirmationProvider provider) {
        final String svcId = credentials.getToolId();
        final String username = secrets.getUsername();
        OperatingMode.LOGGER.debug("Confirmation provider '{}' will be using '{}'.", svcId, provider.getClass());
        return credentials.getToken()
                .map(token -> {
                    secrets.setSecret(svcId, token);
                    return Optional.of(new ZonkyProxy.Builder().usingConfirmation(provider, username, token));
                }).orElseGet(() -> secrets.getSecret(svcId)
                        .map(token -> Optional.of(new ZonkyProxy.Builder().usingConfirmation(provider, username, token)))
                        .orElseGet(() -> {
                            OperatingMode.LOGGER.error("Password not provided for confirmation service '{}'.",
                                    svcId);
                            return Optional.empty();
                        })
                );
    }


    private static Optional<ZonkyProxy.Builder> getZonkyProxyBuilder(final ConfirmationCredentials credentials,
                                                                     final SecretProvider secrets) {
        final String svcId = credentials.getToolId();
        return ConfirmationProviderLoader.load(svcId)
                .map(provider -> OperatingMode.getZonkyProxyBuilder(credentials, secrets, provider))
                .orElseGet(() -> {
                    OperatingMode.LOGGER.error("Confirmation provider '{}' not found, yet it is required.", svcId);
                    return Optional.empty();
        });
    }

    private final Option selectingOption;
    private final Collection<Option> otherOptions;

    OperatingMode(final Option selectingOption, final Option... otherOptions) {
        this.selectingOption = selectingOption;
        this.otherOptions = new LinkedHashSet<>(Arrays.asList(otherOptions));
    }

    /**
     * Option to be selected on the command line in order to activate this mode.
     * @return Option in question.
     */
    public Option getSelectingOption() {
        return selectingOption;
    }

    /**
     * Other options that are valid for this operating mode.
     * @return Options in question.
     */
    public Collection<Option> getOtherOptions() {
        return otherOptions;
    }

    /**
     * Transform the command line into a workable application configuration.
     *
     * @param cli Command line received from the application.
     * @param secrets Provider for the storage of secret data.
     * @return Present if configuration successful.
     */
    public abstract Optional<Configuration> configure(final CommandLineInterface cli, final SecretProvider secrets);

}
