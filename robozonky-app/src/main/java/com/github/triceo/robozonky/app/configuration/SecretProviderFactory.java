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

import java.io.File;
import java.io.IOException;
import java.security.KeyStoreException;
import java.util.Optional;

import com.github.triceo.robozonky.common.secrets.KeyStoreHandler;
import com.github.triceo.robozonky.common.secrets.SecretProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class SecretProviderFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(SecretProviderFactory.class);
    private static final File DEFAULT_KEYSTORE = new File("robozonky.keystore");

    private static SecretProvider newSecretProvider(final String username, final char[] password,
                                                    final File keyStoreLocation) throws IOException, KeyStoreException {
        final KeyStoreHandler ksh = KeyStoreHandler.create(keyStoreLocation, password);
        SecretProviderFactory.LOGGER.info("Guarded storage was created with your username and password: {}",
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
    static Optional<SecretProvider> newSecretProvider(final CommandLine cli, final File defaultKeyStore)
            throws KeyStoreException {
        final Optional<File> keyStoreLocation = cli.getAuthenticationFragment().getKeystore();
        final char[] password = cli.getAuthenticationFragment().getPassword();
        if (keyStoreLocation.isPresent()) { // if user requests keystore, cli is only used to retrieve keystore file
            try {
                return Optional.of(SecretProviderFactory.existingSecretProvider(password, keyStoreLocation.get()));
            } catch (final IOException ex) {
                SecretProviderFactory.LOGGER.error("Failed opening guarded storage.", ex);
                return Optional.empty();
            }
        } else { // else everything is read from the cli and put into a keystore
            final Optional<String> usernameProvided = cli.getAuthenticationFragment().getUsername();
            final boolean storageExists = defaultKeyStore.canRead();
            if (storageExists) {
                if (defaultKeyStore.delete()) { // will create a new guided storage
                    SecretProviderFactory.LOGGER.info("Using plain-text credentials when guarded storage available. " +
                            "Consider switching.");
                } else {
                    SecretProviderFactory.LOGGER.error("Stale guarded storage is present and can not be deleted.");
                    return Optional.empty();
                }
            } else if (!usernameProvided.isPresent()) {
                SecretProviderFactory.LOGGER.error("When not using guarded storage, username must be provided.");
                return Optional.empty();
            }
            try {
                final SecretProvider secrets =
                        SecretProviderFactory.newSecretProvider(usernameProvided.get(), password, defaultKeyStore);
                return Optional.of(secrets);
            } catch (final IOException ex) {
                SecretProviderFactory.LOGGER.error("Failed reading guarded storage.", ex);
                return Optional.empty();
            }
        }
    }

    static Optional<SecretProvider> getFallbackSecretProvider(final CommandLine cli) {
        SecretProviderFactory.LOGGER.info("You should get a better Java runtime.");
        final AuthenticationCommandLineFragment cliAuth = cli.getAuthenticationFragment();
        return cliAuth.getKeystore()
                .map(keyStore -> { // keystore is demanded but apparently unsupported, fail
                    SecretProviderFactory.LOGGER.error("No KeyStore support detected, yet it is demanded.");
                    return Optional.<SecretProvider>empty();
                }).orElseGet(() ->  { // keystore is optional, fall back to other means
                    SecretProviderFactory.LOGGER.warn("No KeyStore support detected, storing password insecurely.");
                    return Optional.of(SecretProvider.fallback(cliAuth.getUsername().get(), cliAuth.getPassword()));
                });
    }

    public static Optional<SecretProvider> getSecretProvider(final CommandLine cli) {
        try {
            return SecretProviderFactory.newSecretProvider(cli, SecretProviderFactory.DEFAULT_KEYSTORE);
        } catch (final KeyStoreException ex) {
            return SecretProviderFactory.getFallbackSecretProvider(cli);
        }

    }

}
