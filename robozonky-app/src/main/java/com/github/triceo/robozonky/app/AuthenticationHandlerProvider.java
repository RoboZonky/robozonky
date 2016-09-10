/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Function;

import com.github.triceo.robozonky.ApiProvider;
import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import com.github.triceo.robozonky.app.util.KeyStoreHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuthenticationHandlerProvider implements Function<CommandLineInterface, Optional<AuthenticationHandler>> {

    private static final File DEFAULT_KEYSTORE_FILE = new File("robozonky.keystore");
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHandlerProvider.class);

    private static SecretProvider newSecretProvider(final String username, final String password,
                                                    final File keyStoreLocation) throws IOException, KeyStoreException {
        final KeyStoreHandler ksh = KeyStoreHandler.create(keyStoreLocation, password);
        AuthenticationHandlerProvider.LOGGER.info("Guarded storage was created with your username and password: {}",
                keyStoreLocation);
        return SecretProvider.keyStoreBased(ksh, username, password);

    }

    private static SecretProvider existingSecretProvider(final String password, final File keyStoreLocation)
            throws IOException, KeyStoreException {
        final KeyStoreHandler ksh = KeyStoreHandler.open(keyStoreLocation, password);
        return SecretProvider.keyStoreBased(ksh);
    }

    static Optional<SecretProvider> getSecretProvider(final CommandLineInterface cli, final File defaultKeyStore) {
        final Optional<File> keyStoreLocation = cli.getKeyStoreLocation();
        final String password = cli.getPassword();
        if (keyStoreLocation.isPresent()) { // if user requests keystore, cli is only used to retrieve keystore file
            try {
                return Optional.of(AuthenticationHandlerProvider.existingSecretProvider(password,
                        keyStoreLocation.get()));
            } catch (final IOException | KeyStoreException ex) {
                AuthenticationHandlerProvider.LOGGER.error("Failed opening guarded storage.", ex);
                return Optional.empty();
            }
        } else { // else everything is read from the cli and put into a keystore
            final Optional<String> usernameProvided = cli.getUsername();
            final boolean storageExists = defaultKeyStore.canRead();
            if (storageExists) {
                if (defaultKeyStore.delete()) { // will create a new guided storage
                    AuthenticationHandlerProvider.LOGGER.info("Using plain-text credentials when guarded storage " +
                            "available. Consider switching.");
                } else {
                    AuthenticationHandlerProvider.LOGGER.error("Stale guarded storage is present and can not be " +
                            "deleted.");
                    return Optional.empty();
                }
            } else if (!usernameProvided.isPresent()) {
                AuthenticationHandlerProvider.LOGGER.error("When not using guarded storage, username must be " +
                        "provided.");
                return Optional.empty();
            }
            try {
                return Optional.of(AuthenticationHandlerProvider.newSecretProvider(usernameProvided.get(), password,
                        defaultKeyStore));
            } catch (final IOException | KeyStoreException ex) {
                AuthenticationHandlerProvider.LOGGER.error("Failed reading guarded storage.", ex);
                return Optional.empty();
            }
        }
    }

    static AuthenticationHandler instantiateAuthenticationHandler(final ApiProvider apiProvider,
                                                                  final SecretProvider secProvider,
                                                                  final CommandLineInterface cli) {
        if (cli.isTokenEnabled()) {
            final Optional<Integer> secs = cli.getTokenRefreshBeforeExpirationInSeconds();
            return secs.isPresent() ?
                    AuthenticationHandler.tokenBased(apiProvider, secProvider, cli.isDryRun(), secs.get(),
                            ChronoUnit.SECONDS) :
                    AuthenticationHandler.tokenBased(apiProvider, secProvider, cli.isDryRun());
        } else {
            return AuthenticationHandler.passwordBased(apiProvider, secProvider, cli.isDryRun());
        }
    }

    private final ApiProvider apiProvider;

    public AuthenticationHandlerProvider(final ApiProvider apiProvider) {
        this.apiProvider = apiProvider;
    }

    @Override
    public Optional<AuthenticationHandler> apply(final CommandLineInterface commandLineInterface) {
        final Optional<SecretProvider> optionalSensitive =
                AuthenticationHandlerProvider.getSecretProvider(commandLineInterface,
                        AuthenticationHandlerProvider.DEFAULT_KEYSTORE_FILE);
        if (!optionalSensitive.isPresent()) {
            return Optional.empty();
        }
        final SecretProvider sensitive = optionalSensitive.get();
        return Optional.of(AuthenticationHandlerProvider.instantiateAuthenticationHandler(this.apiProvider, sensitive,
                commandLineInterface));
    }
}
