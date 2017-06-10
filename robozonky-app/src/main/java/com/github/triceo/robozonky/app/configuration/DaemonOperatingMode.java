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
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.investing.DaemonInvestmentMode;
import com.github.triceo.robozonky.app.investing.InvestmentMode;
import com.github.triceo.robozonky.app.investing.Investor;
import com.github.triceo.robozonky.common.extensions.MarketplaceLoader;
import com.github.triceo.robozonky.common.secrets.Credentials;

@Parameters(commandNames = "daemon", commandDescription = "Constantly checks marketplace, invests based on strategy.")
class DaemonOperatingMode extends OperatingMode {

    @ParametersDelegate
    DaemonModeMarketplaceCommandLineFragment marketplaceFragment = new DaemonModeMarketplaceCommandLineFragment();

    @ParametersDelegate
    StrategyCommandLineFragment strategyFragment = new StrategyCommandLineFragment();

    @Override
    protected Optional<InvestmentMode> getInvestmentMode(final CommandLine cli,
                                                         final AuthenticationHandler auth,
                                                         final Investor.Builder builder) {
        final Refreshable<InvestmentStrategy> strategy =
                RefreshableInvestmentStrategy.create(strategyFragment.getStrategyLocation());
        final TweaksCommandLineFragment fragment = cli.getTweaksFragment();
        final Credentials cred = new Credentials(marketplaceFragment.getMarketplaceCredentials(),
                auth.getSecretProvider());
        return MarketplaceLoader.load(cred)
                .map(marketplace -> {
                    final InvestmentMode m = new DaemonInvestmentMode(auth, builder, fragment.isFaultTolerant(),
                            marketplace, strategy, marketplaceFragment.getMaximumSleepDuration(),
                            marketplaceFragment.getDelayBetweenChecks());
                    return Optional.of(m);
                }).orElse(Optional.empty());
    }

}
