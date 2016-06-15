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
 * class, such as {@link #keyStoreBased(KeyStoreHandler)}.
 */
public abstract class SensitiveInformationProvider {

    /**
     * Create a @{@link KeyStore}-based provider based on a given key store.
     *
     * @param ksh Initialized KeyStore for the provider to work with.
     * @return The provider.
     */
    public static SensitiveInformationProvider keyStoreBased(final KeyStoreHandler ksh) {
        return new KeyStoreInformationProvider(ksh);
    }

    /**
     * Create a @{@link KeyStore}-based provider based on a given key store, filled with initial username and password.
     *
     * @param ksh Initialized KeyStore for the provider to work with.
     * @param username Zonky username to store in the key store.
     * @param password Zonky password to store in the key store.
     * @return The provider.
     */
    public static SensitiveInformationProvider keyStoreBased(final KeyStoreHandler ksh, final String username,
                                                             final String password) {
        final KeyStoreInformationProvider ks
                = (KeyStoreInformationProvider)SensitiveInformationProvider.keyStoreBased(ksh);
        ks.setPassword(password);
        ks.setUsername(username);
        return ks;
    }

    /**
     * Retrieve password used to connect to Zonky API.
     *
     * @return
     */
    abstract public String getPassword();


    /**
     * Retrieve username used to connect to Zonky API.
     *
     * @return
     */
    abstract public String getUsername();

    /**
     * Retrieve serialization of Zonky's OAuth token.
     *
     * @return Present if {@link #setToken(Reader)} previously called, unless {@link #setToken()} was called after
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
    abstract public boolean setToken();

    /**
     * Retrieve the timestamp of the stored token.
     *
     * @return the last time when {@link #setToken(Reader)} was called, unless {@link #setToken()} called after
     * that.
     */
    abstract public Optional<LocalDateTime> getTokenSetDate();

}
