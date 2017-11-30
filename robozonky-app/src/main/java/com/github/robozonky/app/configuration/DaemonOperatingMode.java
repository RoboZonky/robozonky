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

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.beust.jcommander.Parameters;
import com.beust.jcommander.ParametersDelegate;
import com.github.robozonky.api.notifications.RoboZonkyInitializedEvent;
import com.github.robozonky.api.notifications.SessionInfo;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.configuration.daemon.BlockedAmountsUpdater;
import com.github.robozonky.app.configuration.daemon.DaemonInvestmentMode;
import com.github.robozonky.app.configuration.daemon.PortfolioUpdater;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.portfolio.Delinquents;
import com.github.robozonky.common.extensions.MarketplaceLoader;
import com.github.robozonky.common.secrets.Credentials;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.util.Scheduler;

@Parameters(commandNames = "daemon", commandDescription = "Constantly checks marketplaces, invests based on strategy.")
class DaemonOperatingMode extends OperatingMode {

    @ParametersDelegate
    MarketplaceCommandLineFragment marketplace = new MarketplaceCommandLineFragment();

    @ParametersDelegate
    StrategyCommandLineFragment strategy = new StrategyCommandLineFragment();

    private static LocalDateTime getNextFourAM(final LocalDateTime now) {
        final LocalDateTime fourAM = LocalTime.of(4, 0).atDate(now.toLocalDate());
        if (fourAM.isAfter(now)) {
            return fourAM;
        }
        return fourAM.plusDays(1);
    }

    private static Duration timeUntil4am(final LocalDateTime now) {
        final LocalDateTime nextFourAm = getNextFourAM(now);
        return Duration.between(now, nextFourAm);
    }

    @Override
    protected Optional<InvestmentMode> getInvestmentMode(final CommandLine cli, final Authenticated auth,
                                                         final Investor.Builder builder) {
        final boolean isFaultTolerant = cli.getTweaksFragment().isFaultTolerant();
        final SecretProvider secretProvider = auth.getSecretProvider();
        final Credentials cred = new Credentials(marketplace.getMarketplaceCredentials(), secretProvider);
        return MarketplaceLoader.load(cred)
                .map(marketplaceImpl -> {
                    final PortfolioUpdater updater = new PortfolioUpdater(auth);
                    final BlockedAmountsUpdater bau = new BlockedAmountsUpdater(auth, updater);
                    // run update of blocked amounts automatically with every portfolio update
                    updater.registerDependant(bau.getDependant());
                    // update delinquents automatically with every portfolio update
                    updater.registerDependant(new Delinquents((loanId, zonky) -> updater.get()
                            .map(portfolio -> portfolio.getLoan(zonky, loanId))
                            .orElseThrow(() -> new IllegalStateException("Cannot call on empty portfolio."))));
                    final InvestmentMode m = new DaemonInvestmentMode(auth, updater, builder, isFaultTolerant,
                                                                      marketplaceImpl, strategy.getStrategyLocation(),
                                                                      marketplace.getMaximumSleepDuration(),
                                                                      marketplace.getPrimaryMarketplaceCheckDelay(),
                                                                      marketplace.getSecondaryMarketplaceCheckDelay());
                    // initialize SessionInfo before the robot potentially sends the first notification
                    Events.fire(new RoboZonkyInitializedEvent(), new SessionInfo(secretProvider.getUsername()));
                    // only schedule internal data updates after daemon had a chance to initialize...
                    final Scheduler scheduler = Scheduler.inBackground();
                    final Future<?> f = scheduler.run(updater);
                    try {
                        /*
                         * wait for the update to finish; has to be done in this roundabout way, so that integration
                         * tests can substitute this operation, which would otherwise call a live Zonky API, by a no-op
                         * via the pluggable scheduler mechanism.
                         */
                        f.get();
                    } catch (final ExecutionException | InterruptedException ex) {
                        LOGGER.error("Failed updating portfolio.", ex);
                        return Optional.<InvestmentMode>empty();
                    }
                    // ... and then run the update every 12 hours, starting at 4 a.m.
                    scheduler.submit(updater, Duration.ofHours(12), timeUntil4am(LocalDateTime.now()));
                    // also run blocked amounts update every now and then to detect changes made outside of the robot
                    scheduler.submit(bau, Duration.ofHours(1));
                    return Optional.of(m);
                }).orElse(Optional.empty());
    }
}
