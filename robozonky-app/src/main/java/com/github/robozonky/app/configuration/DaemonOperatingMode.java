/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.configuration;

import java.util.Optional;
import java.util.function.Consumer;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.configuration.daemon.DaemonInvestmentMode;
import com.github.robozonky.app.configuration.daemon.PortfolioUpdater;
import com.github.robozonky.app.configuration.daemon.StrategyProvider;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.common.extensions.MarketplaceLoader;
import com.github.robozonky.common.secrets.Credentials;
import com.github.robozonky.common.secrets.SecretProvider;

@Parameters(commandNames = "daemon", commandDescription = "Constantly checks marketplaces, invests based on strategy.")
class DaemonOperatingMode extends OperatingMode {

    private final Consumer<Throwable> shutdownCall;
    @ParametersDelegate
    MarketplaceCommandLineFragment marketplace = new MarketplaceCommandLineFragment();
    @ParametersDelegate
    StrategyCommandLineFragment strategy = new StrategyCommandLineFragment();

    public DaemonOperatingMode(final Consumer<Throwable> shutdownCall) {
        this.shutdownCall = shutdownCall;
    }

    @Override
    protected Optional<InvestmentMode> getInvestmentMode(final CommandLine cli, final Authenticated auth,
                                                         final Investor.Builder builder) {
        final boolean isFaultTolerant = cli.getTweaksFragment().isFaultTolerant();
        if (isFaultTolerant) {
            LOGGER.warn("RoboZonky is now fault tolerant by default and cannot be configured otherwise."
                                + " '-t' command-line option will be removed in the next RoboZonky minor version. " +
                                "Kindly stop using it.");
        }
        final SecretProvider secretProvider = auth.getSecretProvider();
        final String marketplaceId = marketplace.getMarketplaceCredentials();
        if (marketplaceId != null) {
            LOGGER.warn("RoboZonky will soon stop supporting Zotify marketplace cache, instead using Zonky directly."
                                + " '-m' command-line option will be removed in the next RoboZonky minor version. " +
                                "Kindly stop using it.");
        }
        final Credentials cred = new Credentials(marketplaceId == null ? "zonky" : marketplaceId, secretProvider);
        return MarketplaceLoader.load(cred)
                .map(marketplaceImpl -> {
                    final StrategyProvider sp = StrategyProvider.createFor(strategy.getStrategyLocation());
                    final PortfolioUpdater u = PortfolioUpdater.create(shutdownCall, auth, sp, builder.isDryRun());
                    final InvestmentMode m = new DaemonInvestmentMode(auth, u, builder, marketplaceImpl, sp,
                                                                      marketplace.getMaximumSleepDuration(),
                                                                      marketplace.getPrimaryMarketplaceCheckDelay(),
                                                                      marketplace.getSecondaryMarketplaceCheckDelay());
                    return Optional.of(m);
                }).orElse(Optional.empty());
    }
}
