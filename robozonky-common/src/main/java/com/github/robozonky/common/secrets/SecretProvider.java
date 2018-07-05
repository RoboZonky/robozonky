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

package com.github.robozonky.common.secrets;

import java.security.KeyStore;
import java.util.Optional;

/**
 * Implementations provide various ways of storing sensitive information, such as passwords or access tokens.
 * <p>
 * Users shall not be given access to these children - instead, they should get them by calling static methods on this
 * class, such as {@link #keyStoreBased(KeyStoreHandler)}.
 */
public interface SecretProvider {

    /**
     * Create a @{@link KeyStore}-based provider based on a given key store.
     * @param ksh Initialized KeyStore for the provider to work with.
     * @return The provider.
     */
    static SecretProvider keyStoreBased(final KeyStoreHandler ksh) {
        return new KeyStoreSecretProvider(ksh);
    }

    /**
     * Create a @{@link KeyStore}-based provider based on a given key store, filled with initial username and password.
     * @param ksh Initialized KeyStore for the provider to work with.
     * @param user Zonky username to store in the key store.
     * @param password Zonky password to store in the key store.
     * @return The provider.
     */
    static SecretProvider keyStoreBased(final KeyStoreHandler ksh, final String user, final char... password) {
        final KeyStoreSecretProvider ks = (KeyStoreSecretProvider) SecretProvider.keyStoreBased(ksh);
        ks.setPassword(password);
        ks.setUsername(user);
        return ks;
    }

    /**
     * Very useful for testing, as it has no extra dependencies such as keystore files etc.
     * @param username Zonky username.
     * @param password Zonky password.
     * @return The provider.
     */
    static SecretProvider inMemory(final String username, final char... password) {
        return new FallbackSecretProvider(username, password);
    }

    /**
     * Retrieve password used to connect to Zonky API.
     * @return The password.
     */
    char[] getPassword();

    /**
     * Retrieve username used to connect to Zonky API.
     * @return The username.
     */
    String getUsername();

    /**
     * Retrieve a secret stored through {@link #setSecret(String, char[])}.
     * @param secretId ID of the secret to retrieve.
     * @return The secret, if found.
     */
    Optional<char[]> getSecret(final String secretId);

    /**
     * Store a secret.
     * @param secretId ID of the secret.
     * @param secret The secret to store.
     * @return True if successful.
     */
    boolean setSecret(final String secretId, final char... secret);

    /**
     * Whether or not this provider will store all data in such a way that it survives JVM restart.
     * @return True if the storage is persistent.
     */
    boolean isPersistent();
}
