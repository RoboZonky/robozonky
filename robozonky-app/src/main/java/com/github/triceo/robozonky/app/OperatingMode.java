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

import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import org.apache.commons.cli.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;

/**
 * Represents different modes of operation of the application, their means of selection and setup.
 */
enum OperatingMode {

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
        public Optional<AppContext> setup(final CommandLineInterface cli) {
            if (cli.getLoanAmount().isPresent() || cli.getLoanId().isPresent()) {
                cli.printHelp("Loan data makes no sense in this context.", true);
                return Optional.empty();
            }
            final Optional<String> strategyFilePath = cli.getStrategyConfigurationFilePath();
            if (!strategyFilePath.isPresent()) {
                cli.printHelp("Strategy file must be provided.", true);
                return Optional.empty();
            }
            final File strategyConfig = new File(strategyFilePath.get());
            if (!strategyConfig.exists()) {
                cli.printHelp("Investment strategy file does not exist: " + strategyConfig.getAbsolutePath(), true);
                return Optional.empty();
            } else if (!strategyConfig.canRead()) {
                cli.printHelp("Investment strategy file can not be read: " + strategyConfig.getAbsolutePath(), true);
                return Optional.empty();
            } else try {
                final Optional<InvestmentStrategy> strategy = InvestmentStrategy.load(strategyConfig);
                if (!strategy.isPresent()) {
                    OperatingMode.LOGGER.error("No investment strategy found to support {}.",
                            strategyConfig.getAbsolutePath());
                    return Optional.empty();
                } else if (cli.isDryRun()) {
                    final int balance = cli.getDryRunBalance().orElse(-1);
                    return Optional.of(new AppContext(strategy.get(), balance));
                } else {
                    return Optional.of(new AppContext(strategy.get()));
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
        public Optional<AppContext> setup(final CommandLineInterface cli) {
            final Optional<Integer> loanId = cli.getLoanId();
            final Optional<Integer> loanAmount = cli.getLoanAmount();
            if (!loanId.isPresent() || loanId.get() < 1) {
                cli.printHelp("Loan ID must be provided and greater than 0.", true);
                return Optional.empty();
            } else if (!loanAmount.isPresent() || loanAmount.get() < 1) {
                cli.printHelp("Loan amount must be provided and greater than 0.", true);
                return Optional.empty();
            } else if (cli.isDryRun()) {
                final int balance = cli.getDryRunBalance().orElse(-1);
                return Optional.of(new AppContext(loanId.get(), loanAmount.get(), balance));
            } else {
                return Optional.of(new AppContext(loanId.get(), loanAmount.get()));
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
     *
     * @return Option in question.
     */
    public Option getSelectingOption() {
        return selectingOption;
    }

    /**
     * Other options that are valid for this operating mode.
     *
     * @return Options in question.
     */
    public Collection<Option> getOtherOptions() {
        return otherOptions;
    }

    /**
     * Properly set up the application with this operating mode.
     *
     * @param cli Parsed command line.
     * @return All information required for proper execution of the application. Empty on failure.
     */
    public abstract Optional<AppContext> setup(final CommandLineInterface cli);
}
