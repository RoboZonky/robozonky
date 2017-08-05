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

import com.github.triceo.robozonky.api.confirmations.ConfirmationProvider;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.commons.InvestmentMode;
import com.github.triceo.robozonky.app.investing.Investor;
import com.github.triceo.robozonky.common.extensions.ConfirmationProviderLoader;
import com.github.triceo.robozonky.common.secrets.Credentials;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import com.github.triceo.robozonky.internal.api.ToStringBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class OperatingMode implements CommandLineFragment {

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    Optional<Investor.Builder> getZonkyProxyBuilder(final Credentials credentials,
                                                    final SecretProvider secrets,
                                                    final ConfirmationProvider provider) {
        final String svcId = credentials.getToolId();
        LOGGER.debug("Confirmation provider '{}' will be using '{}'.", svcId, provider.getClass());
        credentials.getToken().ifPresent(t -> secrets.setSecret(svcId, t));
        return secrets.getSecret(svcId)
                .map(token -> Optional.of(new Investor.Builder().usingConfirmation(provider, token)))
                .orElseGet(() -> {
                    LOGGER.error("Password not provided for confirmation service '{}'.", svcId);
                    return Optional.empty();
                });
    }

    Optional<Investor.Builder> getZonkyProxyBuilder(final Credentials credentials,
                                                    final SecretProvider secrets) {
        final String svcId = credentials.getToolId();
        return ConfirmationProviderLoader.load(svcId)
                .map(provider -> this.getZonkyProxyBuilder(credentials, secrets, provider))
                .orElseGet(() -> {
                    LOGGER.error("Confirmation provider '{}' not found, yet it is required.", svcId);
                    return Optional.empty();
                });
    }

    protected abstract Optional<InvestmentMode> getInvestmentMode(final CommandLine cli,
                                                                  final Authenticated auth,
                                                                  final Investor.Builder builder);

    public Optional<InvestmentMode> configure(final CommandLine cli, final Authenticated auth) {
        final Optional<Credentials> cred = cli.getConfirmationFragment().getConfirmationCredentials().map(
                value -> new Credentials(value, auth.getSecretProvider()));
        final Optional<Investor.Builder> optionalBuilder = cred
                .map(credentials -> this.getZonkyProxyBuilder(credentials, auth.getSecretProvider()))
                .orElse(Optional.of(new Investor.Builder()));
        return optionalBuilder
                .map(builder -> {
                    if (cli.getTweaksFragment().isDryRunEnabled()) {
                        LOGGER.info("RoboZonky is doing a dry run. It will not invest any real money.");
                        builder.asDryRun();
                    }
                    builder.asUser(auth.getSecretProvider().getUsername());
                    return this.getInvestmentMode(cli, auth, builder);
                })
                .orElse(Optional.empty());
    }

    public String toString() {
        return new ToStringBuilder(this).toString();
    }
}
