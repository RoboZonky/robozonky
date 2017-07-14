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

    /**
     * Obtain keystore-based secret provider if possible.
     *
     * @param cli Command line interface coming from the application.
     * @return KeyStore-based secret provider or empty in case of a problem happened inside the keystore.
     */
    public static Optional<SecretProvider> getSecretProvider(final CommandLine cli) {
        final Optional<File> keyStoreLocation = cli.getAuthenticationFragment().getKeystore();
        final char[] password = cli.getAuthenticationFragment().getPassword();
        return keyStoreLocation.map(keystore -> {
            try {
                final KeyStoreHandler ksh = KeyStoreHandler.open(keyStoreLocation.get(), password);
                return Optional.of(SecretProvider.keyStoreBased(ksh));
            } catch (final IOException ex) {
                SecretProviderFactory.LOGGER.error("Failed opening guarded storage.", ex);
                return Optional.<SecretProvider>empty();
            } catch (final KeyStoreException ex) {
                SecretProviderFactory.LOGGER.info("Can not use keystore.", ex);
                return SecretProviderFactory.getFallbackSecretProvider(cli);
            }
        }).orElse(SecretProviderFactory.getFallbackSecretProvider(cli));
    }

    static Optional<SecretProvider> getFallbackSecretProvider(final CommandLine cli) {
        final AuthenticationCommandLineFragment cliAuth = cli.getAuthenticationFragment();
        return cliAuth.getKeystore().map(keyStore -> { // keystore is demanded but apparently unsupported, fail
            SecretProviderFactory.LOGGER.error("No KeyStore support detected, yet it is demanded.");
            return Optional.<SecretProvider>empty();
        }).orElseGet(() ->  { // keystore is optional, fall back to other means
            SecretProviderFactory.LOGGER.warn("Keystore not being used, storing password insecurely.");
            return Optional.of(SecretProvider.fallback(cliAuth.getUsername().get(), cliAuth.getPassword()));
        });
    }

}
