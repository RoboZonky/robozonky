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
import java.util.Optional;

import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import org.apache.commons.cli.Option;

enum OperatingMode {

    STRATEGY_DRIVEN(CommandLineInterface.OPTION_STRATEGY, CommandLineInterface.OPTION_DRY_RUN) {
        @Override
        public AppContext setup(final CommandLineInterface cli, final AuthenticationHandler auth) {
            if (cli.getLoanAmount().isPresent() || cli.getLoanId().isPresent()) {
                cli.printHelpAndExit("Loan data makes no sense in this context.", true);
                return null;
            }
            final Optional<String> strategyFilePath = cli.getStrategyConfigurationFilePath();
            if (!strategyFilePath.isPresent()) {
                cli.printHelpAndExit("Strategy file must be provided.", true);
                return null;
            }
            final File strategyConfig = new File(strategyFilePath.get());
            if (!strategyConfig.canRead()) {
                cli.printHelpAndExit("Investment strategy file must be readable.", true);
                return null;
            }
            try {
                if (cli.isDryRun()) {
                    return new AppContext(auth, StrategyParser.parse(strategyConfig), cli.getDryRunBalance());
                } else {
                    return new AppContext(auth, StrategyParser.parse(strategyConfig));
                }
            } catch (final Exception ex) {
                cli.printHelpAndExit("Failed parsing strategy.", ex);
                return null;
            }
        }
    },
    USER_DRIVEN(CommandLineInterface.OPTION_INVESTMENT, CommandLineInterface.OPTION_AMOUNT,
            CommandLineInterface.OPTION_DRY_RUN) {
        @Override
        public AppContext setup(final CommandLineInterface cli, final AuthenticationHandler auth) {
            final Optional<Integer> loanId = cli.getLoanId();
            final Optional<Integer> loanAmount = cli.getLoanAmount();
            if (!loanId.isPresent() || loanId.get() < 1) {
                cli.printHelpAndExit("Loan ID must be provided and greater than 0.", true);
                return null;
            } else if (!loanAmount.isPresent() || loanAmount.get() < 1) {
                cli.printHelpAndExit("Loan amount must be provided and greater than 0.", true);
                return null;
            } else if (cli.isDryRun()) {
                return new AppContext(auth, loanId.get(), loanAmount.get(), cli.getDryRunBalance());
            } else {
                return new AppContext(auth, loanId.get(), loanAmount.get());
            }
        }
    };

    private final Option selectingOption;
    private final Collection<Option> otherOptions;

    OperatingMode(final Option selectingOption, final Option... otherOptions) {
        this.selectingOption = selectingOption;
        this.otherOptions = new LinkedHashSet<>(Arrays.asList(otherOptions));
    }

    public Option getSelectingOption() {
        return selectingOption;
    }

    public Collection<Option> getOtherOptions() {
        return otherOptions;
    }

    public abstract AppContext setup(final CommandLineInterface cli, final AuthenticationHandler auth);
}
