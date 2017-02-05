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
import com.beust.jcommander.ParametersDelegate;
import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import com.github.triceo.robozonky.app.investing.DaemonInvestmentMode;
import com.github.triceo.robozonky.app.investing.DirectInvestmentMode;
import com.github.triceo.robozonky.app.investing.InvestmentMode;
import com.github.triceo.robozonky.app.investing.SingleShotInvestmentMode;
import com.github.triceo.robozonky.app.investing.ZonkyProxy;
import com.github.triceo.robozonky.internal.api.Defaults;
import com.github.triceo.robozonky.internal.api.ToStringBuilder;
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
            return "direct";
        }

        @Override
        protected Optional<InvestmentMode> getInvestmentMode(final CommandLineInterface cli,
                                                             final AuthenticationHandler auth,
                                                             final ZonkyProxy.Builder builder) {
            final TweaksCommandLineFragment fragment = cli.getTweaksFragment();
            return Optional.of(new DirectInvestmentMode(auth, builder, fragment.isFaultTolerant(), loanId, loanAmount));
        }

    }, SINGLE_SHOT {

        @ParametersDelegate
        MarketplaceCommandLineFragment marketplaceFragment = new MarketplaceCommandLineFragment();

        @ParametersDelegate
        StrategyCommandLineFragment strategyFragment = new StrategyCommandLineFragment();

        @Override
        public String getName() {
            return "single";
        }

        @Override
        protected Optional<InvestmentMode> getInvestmentMode(final CommandLineInterface cli,
                                                             final AuthenticationHandler auth,
                                                             final ZonkyProxy.Builder builder) {
            final Refreshable<InvestmentStrategy> strategy =
                    RefreshableInvestmentStrategy.create(strategyFragment.getStrategyLocation());
            final TweaksCommandLineFragment fragment = cli.getTweaksFragment();
            final Credentials cred = new Credentials(marketplaceFragment.getMarketplaceCredentials(),
                    auth.getSecretProvider());
            return MarketplaceLoader.load(cred)
                    .map(marketplace -> {
                        final InvestmentMode m = new SingleShotInvestmentMode(auth, builder, fragment.isFaultTolerant(),
                                marketplace, strategy);
                        return Optional.of(m);
                    })
                    .orElse(Optional.empty());

        }

    }, DAEMON {

        @ParametersDelegate
        MarketplaceCommandLineFragment marketplaceFragment = new MarketplaceCommandLineFragment();

        @ParametersDelegate
        StrategyCommandLineFragment strategyFragment = new StrategyCommandLineFragment();

        @Override
        public String getName() {
            return "daemon";
        }

        @Override
        protected Optional<InvestmentMode> getInvestmentMode(final CommandLineInterface cli,
                                                             final AuthenticationHandler auth,
                                                             final ZonkyProxy.Builder builder) {
            final Refreshable<InvestmentStrategy> strategy =
                    RefreshableInvestmentStrategy.create(strategyFragment.getStrategyLocation());
            final TweaksCommandLineFragment fragment = cli.getTweaksFragment();
            final Credentials cred = new Credentials(marketplaceFragment.getMarketplaceCredentials(),
                    auth.getSecretProvider());
            return MarketplaceLoader.load(cred)
                    .map(marketplace -> {
                        final InvestmentMode m = new DaemonInvestmentMode(auth, builder, fragment.isFaultTolerant(),
                                marketplace, strategy);
                        return Optional.of(m);
                    })
                    .orElse(Optional.empty());

        }

    };

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatingMode.class);

    static Optional<ZonkyProxy.Builder> getZonkyProxyBuilder(final Credentials credentials,
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


    static Optional<ZonkyProxy.Builder> getZonkyProxyBuilder(final Credentials credentials,
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

    protected abstract Optional<InvestmentMode> getInvestmentMode(final CommandLineInterface cli,
                                                                  final AuthenticationHandler auth,
                                                                  final ZonkyProxy.Builder builder);

    public Optional<InvestmentMode> configure(final CommandLineInterface cli, final AuthenticationHandler auth) {
        final Optional<Credentials> cred = cli.getConfirmationFragment().getConfirmationCredentials()
                .map(value -> Optional.of(new Credentials(value, auth.getSecretProvider())))
                .orElse(Optional.empty());
        final Optional<ZonkyProxy.Builder> optionalBuilder = cred
                .map(credentials -> OperatingMode.getZonkyProxyBuilder(credentials, auth.getSecretProvider()))
                .orElse(Optional.of(new ZonkyProxy.Builder()));
        return optionalBuilder
                .map(builder -> {
                    if (cli.getTweaksFragment().isDryRunEnabled()) {
                        OperatingMode.LOGGER.info("RoboZonky is doing a dry run. It will not invest any real money.");
                        builder.asDryRun();
                    }
                    return this.getInvestmentMode(cli, auth, builder);
                })
                .orElse(Optional.empty());
    }

    public String toString() {
        return new ToStringBuilder(this).toString();
    }

}
