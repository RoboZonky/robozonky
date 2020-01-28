/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.secrets;

import java.util.Optional;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Every set*() operation must result in a {@link KeyStoreHandler#save()} call.
 */
final class KeyStoreSecretProvider implements SecretProvider {

    private static final Logger LOGGER = LogManager.getLogger(KeyStoreSecretProvider.class);

    private static final String ALIAS_PASSWORD = "pwd";
    private static final String ALIAS_USERNAME = "usr";
    private static final String ALIAS_TOKEN = "tkn";
    private final KeyStoreHandler ksh;

    public KeyStoreSecretProvider(final KeyStoreHandler ksh) {
        if (ksh == null) {
            throw new IllegalArgumentException("KeyStoreHandler must be provided.");
        }
        this.ksh = ksh;
    }

    /**
     * Set a key in the key store.
     * @param alias Alias to store the key under.
     * @param value Will be stored.
     * @return True if success.
     */
    private boolean set(final String alias, final String value) {
        return this.set(alias, value.toCharArray());
    }

    /**
     * Set a key in the key store.
     * @param alias Alias to store the key under.
     * @param value Will be stored.
     * @return True if success.
     */
    private boolean set(final String alias, final char... value) {
        try {
            final boolean result = this.ksh.set(alias, value);
            this.ksh.save();
            return result;
        } catch (final Exception ex) {
            LOGGER.warn("Failed saving keystore.", ex);
            return false;
        }
    }

    @Override
    public char[] getPassword() {
        return this.ksh.get(ALIAS_PASSWORD)
                .orElseThrow(() -> new IllegalStateException("Password not present in KeyStore."));
    }

    @Override
    public String getUsername() {
        return new String(this.ksh.get(ALIAS_USERNAME)
                                  .orElseThrow(() -> new IllegalStateException("Username not present in KeyStore.")));
    }

    @Override
    public Optional<ZonkyApiToken> getToken() {
        try {
            final ZonkyApiToken result = this.ksh.get(ALIAS_TOKEN)
                    .map(String::new)
                    .map(ZonkyApiToken::unmarshal)
                    .orElse(null);
            return Optional.ofNullable(result);
        } catch (final Exception ex) {
            LOGGER.warn("Failed reading Zonky API token from the keystore.", ex);
            return Optional.empty();
        }
    }

    @Override
    public boolean setToken(ZonkyApiToken apiToken) {
        try {
            if (apiToken == null) {
                this.ksh.delete(ALIAS_TOKEN);
                this.ksh.save();
                return true;
            } else {
                return this.set(ALIAS_TOKEN, ZonkyApiToken.marshal(apiToken).toCharArray());
            }
        } catch (final Exception ex) {
            LOGGER.warn("Failed storing Zonky API token to the keystore.", ex);
            return false;
        }
    }

    public boolean setPassword(final char[] password) {
        return this.set(ALIAS_PASSWORD, password);
    }

    public boolean setUsername(final String username) {
        return this.set(ALIAS_USERNAME, username);
    }

    @Override
    public boolean isPersistent() {
        return true;
    }
}
