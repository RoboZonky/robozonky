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

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.github.triceo.robozonky.api.Refreshable;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.PurchaseStrategy;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.configuration.daemon.DaemonInvestmentMode;
import com.github.triceo.robozonky.app.investing.Investor;
import com.github.triceo.robozonky.common.extensions.MarketplaceLoader;
import com.github.triceo.robozonky.common.secrets.Credentials;

@Parameters(commandNames = "daemon", commandDescription = "Constantly checks marketplaces, invests based on strategy.")
class DaemonOperatingMode extends OperatingMode {

    @ParametersDelegate
    MarketplaceCommandLineFragment marketplaceFragment = new MarketplaceCommandLineFragment();

    @ParametersDelegate
    StrategyCommandLineFragment strategyFragment = new StrategyCommandLineFragment();

    @Override
    protected Optional<InvestmentMode> getInvestmentMode(final CommandLine cli, final Authenticated auth,
                                                         final Investor.Builder builder) {
        final Refreshable<InvestmentStrategy> strategy1 =
                RefreshableInvestmentStrategy.create(strategyFragment.getStrategyLocation());
        final Refreshable<PurchaseStrategy> strategy2 =
                RefreshablePurchaseStrategy.create(strategyFragment.getStrategyLocation());
        final boolean isFaultTolerant = cli.getTweaksFragment().isFaultTolerant();
        final Credentials cred = new Credentials(marketplaceFragment.getMarketplaceCredentials(),
                                                 auth.getSecretProvider());
        return MarketplaceLoader.load(cred)
                .map(marketplace -> {
                    final InvestmentMode m = new DaemonInvestmentMode(auth, builder, isFaultTolerant, marketplace,
                                                                      strategy1, strategy2,
                                                                      marketplaceFragment.getMaximumSleepDuration(),
                                                                      marketplaceFragment.getDelayBetweenChecks());
                    return Optional.of(m);
                }).orElse(Optional.empty());
    }
}
