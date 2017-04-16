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

import java.util.Optional;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.investing.DirectInvestmentMode;
import com.github.triceo.robozonky.app.investing.InvestmentMode;
import com.github.triceo.robozonky.app.investing.ZonkyProxy;
import com.github.triceo.robozonky.internal.api.Defaults;

@Parameters(commandNames = "direct", commandDescription = "Specify a single loan to invest into.")
class DirectInvestmentOperatingMode extends OperatingMode {

    @Parameter(names = {"-l", "--loan"}, required = true,
            description = "ID of loan to invest into.",
            validateValueWith = PositiveIntegerValueValidator.class)
    Integer loanId = 0;

    @Parameter(names = {"-a", "--amount"},
            description = "Amount to invest.",
            validateValueWith = PositiveIntegerValueValidator.class)
    Integer loanAmount = Defaults.MINIMUM_INVESTMENT_IN_CZK;

    @Override
    protected Optional<InvestmentMode> getInvestmentMode(final CommandLineInterface cli,
                                                         final AuthenticationHandler auth,
                                                         final ZonkyProxy.Builder builder) {
        final TweaksCommandLineFragment fragment = cli.getTweaksFragment();
        return Optional.of(new DirectInvestmentMode(auth, builder, fragment.isFaultTolerant(), loanId, loanAmount));
    }

}
