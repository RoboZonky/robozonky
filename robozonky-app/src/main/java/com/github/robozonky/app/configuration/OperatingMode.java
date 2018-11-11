/*
 * Copyright 2018 The RoboZonky Project
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
import java.util.Optional;
import java.util.function.Consumer;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.app.authentication.TenantBuilder;
import com.github.robozonky.app.daemon.DaemonInvestmentMode;
import com.github.robozonky.app.daemon.StrategyProvider;
import com.github.robozonky.app.daemon.operations.Investor;
import com.github.robozonky.app.events.Events;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.extensions.ConfirmationProviderLoader;
import com.github.robozonky.common.extensions.ListenerServiceLoader;
import com.github.robozonky.common.secrets.Credentials;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class OperatingMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(OperatingMode.class);

    private final Consumer<Throwable> shutdownCall;

    OperatingMode(final Consumer<Throwable> shutdownCall) {
        this.shutdownCall = shutdownCall;
    }

    private static Tenant getAuthenticated(final CommandLine cli, final SecretProvider secrets) {
        final Duration duration = Settings.INSTANCE.getTokenRefreshPeriod();
        final TenantBuilder b = new TenantBuilder();
        if (cli.isDryRunEnabled()) {
            LOGGER.info("RoboZonky is doing a dry run. It will not invest any real money.");
            b.dryRun();
        }
        return b.withSecrets(secrets)
                .named(cli.getName())
                .build(duration);
    }

    private static Optional<Investor> getInvestor(final Tenant tenant, final Credentials credentials,
                                                  final ConfirmationProvider provider) {
        final String svcId = credentials.getToolId();
        LOGGER.debug("Confirmation provider '{}' will be using '{}'.", svcId, provider.getClass());
        return credentials.getToken()
                .map(token -> Optional.of(Investor.build(tenant, provider, token)))
                .orElseGet(() -> {
                    LOGGER.error("Password not provided for confirmation service '{}'.", svcId);
                    return Optional.empty();
                });
    }

    private static Optional<Investor> getInvestor(final Tenant tenant, final Credentials credentials) {
        final String svcId = credentials.getToolId();
        return ConfirmationProviderLoader.load(svcId)
                .map(provider -> OperatingMode.getInvestor(tenant, credentials, provider))
                .orElseGet(() -> {
                    LOGGER.error("Confirmation provider '{}' not found, yet it is required.", svcId);
                    return Optional.empty();
                });
    }

    private static void configureNotifications(final CommandLine cli, final SessionInfo session) {
        // unregister if registered
        ListenerServiceLoader.unregisterConfiguration(session);
        // register if needed
        cli.getNotificationConfigLocation().ifPresent(cfg -> ListenerServiceLoader.registerConfiguration(session, cfg));
        // create event handler for this session, otherwise session-less notifications will not be sent
        final Events e = Events.forSession(session);
        LOGGER.debug("Notification subsystem initialized: {}.", e);
    }

    private Optional<InvestmentMode> getInvestmentMode(final CommandLine cli, final Tenant auth,
                                                       final Investor investor) {
        final StrategyProvider sp = StrategyProvider.createFor(cli.getStrategyLocation());
        final InvestmentMode m = new DaemonInvestmentMode(cli.getName(), shutdownCall, auth, investor, sp,
                                                          cli.getPrimaryMarketplaceCheckDelay(),
                                                          cli.getSecondaryMarketplaceCheckDelay());
        return Optional.of(m);
    }

    public Optional<InvestmentMode> configure(final CommandLine cli, final SecretProvider secrets) {
        final Tenant tenant = getAuthenticated(cli, secrets);
        configureNotifications(cli, tenant.getSessionInfo());
        // and now initialize the chosen mode of operation
        return cli.getConfirmationCredentials()
                .map(value -> new Credentials(value, secrets))
                .map(c -> OperatingMode.getInvestor(tenant, c))
                .orElse(Optional.of(Investor.build(tenant)))
                .flatMap(i -> this.getInvestmentMode(cli, tenant, i));
    }
}
