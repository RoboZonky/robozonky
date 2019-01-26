/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.common.secrets.KeyStoreHandler;
import com.github.robozonky.common.secrets.SecretProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class SecretProviderFactory {

    private static final Logger LOGGER = LogManager.getLogger(SecretProviderFactory.class);

    private SecretProviderFactory() {
        // no instances
    }

    /**
     * Obtain keystore-based secret provider if possible.
     * @param cli Command line interface coming from the application.
     * @return KeyStore-based secret provider or empty in case of a problem happened inside the keystore.
     */
    public static Optional<SecretProvider> getSecretProvider(final CommandLine cli) {
        final char[] password = cli.getPassword();
        return cli.getKeystore().map(keystore -> {
            try {
                final KeyStoreHandler ksh = KeyStoreHandler.open(keystore, password);
                return Optional.of(SecretProvider.keyStoreBased(ksh));
            } catch (final Exception ex) {
                LOGGER.error("Failed opening guarded storage.", ex);
                return Optional.<SecretProvider>empty();
            }
        }).orElseThrow(() -> new IllegalStateException("Could not find keystore."));
    }
}
