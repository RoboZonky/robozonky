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

package com.github.triceo.robozonky.app.configuration;

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;

import com.github.triceo.robozonky.app.authentication.AuthenticationHandler;
import com.github.triceo.robozonky.app.authentication.SecretProvider;
import com.github.triceo.robozonky.app.util.KeyStoreHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class AuthenticationHandlerProvider implements Function<CommandLineInterface, Optional<AuthenticationHandler>> {

    private static final File DEFAULT_KEYSTORE = new File("robozonky.keystore");
    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationHandlerProvider.class);

    private static SecretProvider newSecretProvider(final String username, final char[] password,
                                                    final File keyStoreLocation) throws IOException, KeyStoreException {
        final KeyStoreHandler ksh = KeyStoreHandler.create(keyStoreLocation, password);
        AuthenticationHandlerProvider.LOGGER.info("Guarded storage was created with your username and password: {}",
                keyStoreLocation);
        return SecretProvider.keyStoreBased(ksh, username, password);

    }

    private static SecretProvider existingSecretProvider(final char[] password, final File keyStoreLocation)
            throws IOException, KeyStoreException {
        final KeyStoreHandler ksh = KeyStoreHandler.open(keyStoreLocation, password);
        return SecretProvider.keyStoreBased(ksh);
    }

    /**
     * Obtain keystore-based secret provider if possible.
     *
     * @param cli Command line interface coming from the application.
     * @param defaultKeyStore The default file to store the keystore in when the user specified none.
     * @return KeyStore-based secret provider or empty in case of a problem happened inside the keystore.
     * @throws KeyStoreException In case keystores are unsupported on this JRE. Unlikely with Java 8, but seen happen.
     */
    static Optional<SecretProvider> getSecretProvider(final CommandLineInterface cli, final File defaultKeyStore)
            throws KeyStoreException {
        final Optional<File> keyStoreLocation = cli.getKeyStoreLocation();
        final char[] password = cli.getPassword();
        if (keyStoreLocation.isPresent()) { // if user requests keystore, cli is only used to retrieve keystore file
            try {
                return Optional.of(AuthenticationHandlerProvider.existingSecretProvider(password,
                        keyStoreLocation.get()));
            } catch (final IOException ex) {
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
            } catch (final IOException ex) {
                AuthenticationHandlerProvider.LOGGER.error("Failed reading guarded storage.", ex);
                return Optional.empty();
            }
        }
    }

    static AuthenticationHandler instantiateAuthenticationHandler(final SecretProvider secProvider,
                                                                  final CommandLineInterface cli) {
        if (cli.isTokenEnabled()) {
            final OptionalInt secs = cli.getTokenRefreshBeforeExpirationInSeconds();
            return secs.isPresent() ?
                    AuthenticationHandler.tokenBased(secProvider, secs.getAsInt(), ChronoUnit.SECONDS) :
                    AuthenticationHandler.tokenBased(secProvider);
        } else {
            return AuthenticationHandler.passwordBased(secProvider);
        }
    }

    @Override
    public Optional<AuthenticationHandler> apply(final CommandLineInterface cli) {
        try {
            final Optional<SecretProvider> optionalSensitive =
                    AuthenticationHandlerProvider.getSecretProvider(cli, AuthenticationHandlerProvider.DEFAULT_KEYSTORE);
            if (optionalSensitive.isPresent()) {
                final SecretProvider sensitive = optionalSensitive.get();
                return Optional.of(AuthenticationHandlerProvider.instantiateAuthenticationHandler(sensitive, cli));
            } else {
                return Optional.empty();
            }
        } catch (final KeyStoreException ex) {
            AuthenticationHandlerProvider.LOGGER.info("You should get a better Java runtime.");
            final Optional<File> optionalKeyStore = cli.getKeyStoreLocation();
            if (optionalKeyStore.isPresent()) { // keystore is demanded but apparently unsupported, fail
                AuthenticationHandlerProvider.LOGGER.error("No KeyStore support detected, yet it is demanded.");
                return Optional.empty();
            } else { // keystore is optional, fall back to other means
                AuthenticationHandlerProvider.LOGGER.warn("No KeyStore support detected, storing password insecurely.");
                final SecretProvider sensitive = SecretProvider.fallback(cli.getUsername().get(), cli.getPassword());
                return Optional.of(AuthenticationHandlerProvider.instantiateAuthenticationHandler(sensitive, cli));
            }
        }
    }
}
