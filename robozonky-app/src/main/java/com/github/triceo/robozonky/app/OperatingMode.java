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

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashSet;

import org.apache.commons.cli.Option;

enum OperatingMode {

    HELP(CommandLineInterface.OPTION_HELP),
    STRATEGY_DRIVEN(CommandLineInterface.OPTION_STRATEGY, CommandLineInterface.OPTION_USERNAME,
            CommandLineInterface.OPTION_PASSWORD, CommandLineInterface.OPTION_DRY_RUN),
    USER_DRIVER(CommandLineInterface.OPTION_INVESTMENT, CommandLineInterface.OPTION_AMOUNT,
            CommandLineInterface.OPTION_USERNAME, CommandLineInterface.OPTION_PASSWORD,
            CommandLineInterface.OPTION_DRY_RUN);

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
}
