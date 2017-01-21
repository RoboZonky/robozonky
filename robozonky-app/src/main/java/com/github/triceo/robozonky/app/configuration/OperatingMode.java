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
import com.github.triceo.robozonky.ZonkyProxy;
import com.github.triceo.robozonky.api.Defaults;
import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters
enum OperatingMode implements CommandLineFragment {

    DIRECT_INVESTMENT {

        @Parameter(names = {"-l", "--loan"}, required = true,
                description = "ID of loan to invest into.",
                validateValueWith = PositiveIntegerValueValidator.class)
        Integer loanId = 0;

        @Parameter(names = {"-a", "--amount"},
                description = "Amount to invest.",
                validateValueWith = PositiveIntegerValueValidator.class)
        Integer loanAmount = Defaults.MINIMUM_INVESTMENT_IN_CZK;

        @Override
        public String getName() {
            return "single";
        }

        @Override
        protected Configuration getConfiguration(final CommandLineInterface cli, final AuthenticationHandler auth,
                                                 final ZonkyProxy.Builder builder) {
            return new Configuration(loanId, loanAmount, auth, builder, cli.getTweaksFragment().isFaultTolerant(),
                    cli.getTweaksFragment().isDryRunEnabled());
        }

    }, STRATEGY_BASED {

        @Parameter(names = {"-l", "--location"}, required = true,
                description = "Points to a resource holding the investment strategy configuration.")
        String strategyLocation = "";

        @Parameter(names = {"-z", "--zonk"},
                description = "Allows to override the default length of sleep period in minutes.",
                validateValueWith = PositiveIntegerValueValidator.class)
        Integer sleepPeriodInMinutes = 60;

        @Override
        public String getName() {
            return "many";
        }

        @Override
        protected Configuration getConfiguration(final CommandLineInterface cli, final AuthenticationHandler auth,
                                                 final ZonkyProxy.Builder builder) {
            final Refreshable<InvestmentStrategy> strategy = RefreshableInvestmentStrategy.create(strategyLocation);
            final TweaksCommandLineFragment fragment = cli.getTweaksFragment();
            return new Configuration(strategy, auth, builder, sleepPeriodInMinutes, fragment.isFaultTolerant(),
                    fragment.isDryRunEnabled());
        }

    };

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatingMode.class);

    static Optional<ZonkyProxy.Builder> getZonkyProxyBuilder(final ConfirmationCredentials credentials,
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
                        .map(token ->
                                Optional.of(new ZonkyProxy.Builder().usingConfirmation(provider, username, token)))
                        .orElseGet(() -> {
                            OperatingMode.LOGGER.error("Password not provided for confirmation service '{}'.", svcId);
                            return Optional.empty();
                        })
                );
    }


    static Optional<ZonkyProxy.Builder> getZonkyProxyBuilder(final ConfirmationCredentials credentials,
                                                             final SecretProvider secrets) {
        final String svcId = credentials.getToolId();
        return ConfirmationProviderLoader.load(svcId)
                .map(provider -> OperatingMode.getZonkyProxyBuilder(credentials, secrets, provider))
                .orElseGet(() -> {
                    OperatingMode.LOGGER.error("Confirmation provider '{}' not found, yet it is required.", svcId);
                    return Optional.empty();
                });
    }

    public abstract String getName();

    protected abstract Configuration getConfiguration(final CommandLineInterface cli, final AuthenticationHandler auth,
                                                      final ZonkyProxy.Builder builder);

    public Optional<Configuration> configure(final CommandLineInterface cli, final AuthenticationHandler auth) {
        final Optional<ConfirmationCredentials> cred = cli.getConfirmationFragment().getConfirmationCredentials()
                .map(value -> Optional.of(new ConfirmationCredentials(value)))
                .orElse(Optional.empty());
        final Optional<ZonkyProxy.Builder> optionalBuilder = cred
                .map(credentials -> OperatingMode.getZonkyProxyBuilder(credentials, auth.getSecretProvider()))
                .orElse(Optional.of(new ZonkyProxy.Builder()));
        return optionalBuilder
                .map(builder -> Optional.of(this.getConfiguration(cli, auth, builder)))
                .orElse(Optional.empty());
    }

}
