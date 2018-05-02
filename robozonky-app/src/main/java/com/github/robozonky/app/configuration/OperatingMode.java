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
import java.util.Optional;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.confirmations.ConfirmationProvider;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.authentication.TenantBuilder;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.common.extensions.ConfirmationProviderLoader;
import com.github.robozonky.common.secrets.Credentials;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Settings;
import com.github.robozonky.internal.api.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class OperatingMode implements CommandLineFragment {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    private static Tenant getAuthenticated(final CommandLine cli, final SecretProvider secrets) {
        final Duration duration = Settings.INSTANCE.getTokenRefreshPeriod();
        final TenantBuilder b = new TenantBuilder();
        if (cli.getTweaksFragment().isDryRunEnabled()) {
            b.dryRun();
        }
        return b.withSecrets(secrets)
                .named(cli.getName())
                .build(duration);
    }

    Optional<Investor.Builder> getZonkyProxyBuilder(final Credentials credentials,
                                                    final ConfirmationProvider provider) {
        final String svcId = credentials.getToolId();
        LOGGER.debug("Confirmation provider '{}' will be using '{}'.", svcId, provider.getClass());
        return credentials.getToken()
                .map(token -> Optional.of(new Investor.Builder().usingConfirmation(provider, token)))
                .orElseGet(() -> {
                    LOGGER.error("Password not provided for confirmation service '{}'.", svcId);
                    return Optional.empty();
                });
    }

    Optional<Investor.Builder> getZonkyProxyBuilder(final Credentials credentials) {
        final String svcId = credentials.getToolId();
        return ConfirmationProviderLoader.load(svcId)
                .map(provider -> this.getZonkyProxyBuilder(credentials, provider))
                .orElseGet(() -> {
                    LOGGER.error("Confirmation provider '{}' not found, yet it is required.", svcId);
                    return Optional.empty();
                });
    }

    protected abstract Optional<InvestmentMode> getInvestmentMode(final Tenant auth,
                                                                  final Investor.Builder builder);

    public Optional<InvestmentMode> configure(final CommandLine cli, final SecretProvider secrets) {
        final Tenant auth = getAuthenticated(cli, secrets);
        // initialize SessionInfo before the robot potentially sends the first notification
        final SessionInfo s = auth.getSessionInfo();
        Events.initialize(auth.getSessionInfo());
        // and now initialize the chosen mode of operation
        return cli.getConfirmationFragment().getConfirmationCredentials()
                .map(value -> new Credentials(value, secrets))
                .map(this::getZonkyProxyBuilder)
                .orElse(Optional.of(new Investor.Builder()))
                .map(builder -> {
                    if (s.isDryRun()) {
                        LOGGER.info("RoboZonky is doing a dry run. It will not invest any real money.");
                        builder.asDryRun();
                    }
                    builder.asUser(s.getUsername());
                    return this.getInvestmentMode(auth, builder);
                }).orElse(Optional.empty());
    }

    public String toString() {
        return new ToStringBuilder(this).toString();
    }
}
