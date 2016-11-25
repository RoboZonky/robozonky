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
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import com.github.triceo.robozonky.api.events.EventRegistry;
import com.github.triceo.robozonky.api.events.StrategyIdentifiedEvent;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategyParseException;
import org.apache.commons.cli.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Represents different modes of operation of the application, their means of selection and apply.
 */
enum OperatingMode implements Function<CommandLineInterface, Optional<Configuration>> {

    /**
     * Requires a strategy and performs 0 or more investments based on the strategy.
     */
    STRATEGY_DRIVEN(CommandLineInterface.OPTION_STRATEGY, CommandLineInterface.OPTION_DRY_RUN) {
        /**
         *
         * @param cli Parsed command line.
         * @return Empty if strategy missing, not loaded or not parsed.
         */
        @Override
        public Optional<Configuration> apply(final CommandLineInterface cli) {
            if (cli.getLoanAmount().isPresent() || cli.getLoanId().isPresent()) {
                cli.printHelp("Loan data makes no sense in this context.", true);
                return Optional.empty();
            }
            final Optional<String> strategyLocation = cli.getStrategyConfigurationLocation();
            if (!strategyLocation.isPresent()) {
                cli.printHelp("Strategy file must be provided.", true);
                return Optional.empty();
            }
            try {
                final Optional<InvestmentStrategy> strategy = InvestmentStrategy.load(strategyLocation.get());
                if (!strategy.isPresent()) {
                    OperatingMode.LOGGER.error("No investment strategy found to support {}.", strategyLocation);
                    return Optional.empty();
                }
                EventRegistry.fire((StrategyIdentifiedEvent) () -> strategy.get());
                if (cli.isDryRun()) {
                    final int balance = cli.getDryRunBalance().orElse(-1);
                    return Optional.of(new Configuration(strategy.get(), cli.getMaximumSleepPeriodInMinutes(),
                            cli.getCaptchaPreventingInvestingDelayInSeconds(), balance));
                } else {
                    return Optional.of(new Configuration(strategy.get(), cli.getMaximumSleepPeriodInMinutes(),
                            cli.getCaptchaPreventingInvestingDelayInSeconds()));
                }
            } catch (final InvestmentStrategyParseException ex) {
                OperatingMode.LOGGER.error("Failed parsing strategy.", ex);
                return Optional.empty();
            }
        }
    },
    /**
     * Requires a loan ID, into which it will invest a given amount and terminate.
     */
    USER_DRIVEN(CommandLineInterface.OPTION_INVESTMENT, CommandLineInterface.OPTION_AMOUNT,
            CommandLineInterface.OPTION_DRY_RUN) {
        /**
         *
         * @param cli Parsed command line.
         * @return Empty when loan ID or loan amount are empty or missing.
         */
        @Override
        public Optional<Configuration> apply(final CommandLineInterface cli) {
            final OptionalInt loanId = cli.getLoanId();
            final OptionalInt loanAmount = cli.getLoanAmount();
            if (!loanId.isPresent() || loanId.getAsInt() < 1) {
                cli.printHelp("Loan ID must be provided and greater than 0.", true);
                return Optional.empty();
            } else if (!loanAmount.isPresent() || loanAmount.getAsInt() < 1) {
                cli.printHelp("Loan amount must be provided and greater than 0.", true);
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

}
