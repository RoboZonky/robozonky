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

package com.github.triceo.robozonky.app.authentication;

import java.io.Reader;
import java.security.KeyStore;
import java.time.LocalDateTime;
import java.util.Optional;

import com.github.triceo.robozonky.app.util.KeyStoreHandler;

/**
 * Children of this class implement various ways of providing sensitive information, such as passwords or access tokens.
 *
 * Users shall not be given access to these children - instead, they should get them by calling static methods on this
 * class, such as {@link #keyStoreBased(KeyStoreHandler)} or {@link #fallback(String, char[])}.
 */
public abstract class SecretProvider {

    /**
     * Create a @{@link KeyStore}-based provider based on a given key store.
     *
     * @param ksh Initialized KeyStore for the provider to work with.
     * @return The provider.
     */
    public static SecretProvider keyStoreBased(final KeyStoreHandler ksh) {
        return new KeyStoreSecretProvider(ksh);
    }

    /**
     * Create a @{@link KeyStore}-based provider based on a given key store, filled with initial username and password.
     *
     * @param ksh Initialized KeyStore for the provider to work with.
     * @param user Zonky username to store in the key store.
     * @param password Zonky password to store in the key store.
     * @return The provider.
     */
    public static SecretProvider keyStoreBased(final KeyStoreHandler ksh, final String user, final char[] password) {
        final KeyStoreSecretProvider ks = (KeyStoreSecretProvider) SecretProvider.keyStoreBased(ksh);
        ks.setPassword(password);
        ks.setUsername(user);
        return ks;
    }

    /**
     * For cases where there is no KeyStore support available in JDK, this secret provider stores all secrets in plain
     * text.
     * @param username Zonky username.
     * @param password Zonky password.
     * @return The provider.
     */
    public static SecretProvider fallback(final String username, final char[] password) {
        return new FallbackSecretProvider(username, password);
    }

    /**
     * Retrieve password used to connect to Zonky API.
     *
     * @return The password.
     */
    abstract public char[] getPassword();


    /**
     * Retrieve username used to connect to Zonky API.
     *
     * @return The username.
     */
    abstract public String getUsername();

    /**
     * Retrieve serialization of Zonky's OAuth token.
     *
     * @return Present if {@link #setToken(Reader)} previously called, unless {@link #deleteToken()} was called after
     * that.
     */
    abstract public Optional<Reader> getToken();

    /**
     * Store serialization of Zonky's OAuth token.
     *
     * @param token The serialization of the token to be stored.
     * @return True if successful.
     */
    abstract public boolean setToken(final Reader token);

    /**
     * Delete the stored token, if any.
     *
     * @return True if no token stored anymore.
     */
    abstract public boolean deleteToken();

    /**
     * Retrieve the timestamp of the stored token.
     *
     * @return the last time when {@link #setToken(Reader)} was called, unless {@link #deleteToken()} called after
     * that.
     */
    abstract public Optional<LocalDateTime> getTokenSetDate();

}
