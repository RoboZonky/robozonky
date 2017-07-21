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

package com.github.triceo.robozonky.common.secrets;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.triceo.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plain-text secret storage. Should only be used as fallback in case the JDK does not support KeyStores. This is
 * unlikely, but we've seen that happen. (See https://github.com/triceo/robozonky/issues/52.)
 */
final class FallbackSecretProvider implements SecretProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackSecretProvider.class);
    private static final State.ClassSpecificState STATE = State.INSTANCE.forClass(FallbackSecretProvider.class);
    private static final String TOKEN_STATE_ID = "token";

    private final String username;
    private final char[] password;
    private final Map<String, char[]> secrets = new HashMap<>();

    public FallbackSecretProvider(final String username, final char... password) {
        this.username = username;
        this.password = Arrays.copyOf(password, password.length);
    }

    @Override
    public char[] getPassword() {
        return this.password;
    }

    @Override
    public String getUsername() {
        return this.username;
    }

    @Override
    public Optional<Reader> getToken() {
        return FallbackSecretProvider.STATE.getValue(FallbackSecretProvider.TOKEN_STATE_ID)
                .map(o -> Optional.of((Reader) new StringReader(o)))
                .orElse(Optional.empty());
    }

    @Override
    public boolean setToken(final Reader token) {
        try (final BufferedReader r = new BufferedReader(token)) {
            FallbackSecretProvider.STATE.setValue(FallbackSecretProvider.TOKEN_STATE_ID, r.readLine());
            return true;
        } catch (final IOException ex) {
            FallbackSecretProvider.LOGGER.warn("Failed setting token.", ex);
            return false;
        }
    }

    @Override
    public Optional<char[]> getSecret(final String secretId) {
        return Optional.ofNullable(this.secrets.get(secretId));
    }

    @Override
    public boolean setSecret(final String secretId, final char... secret) {
        this.secrets.put(secretId, secret);
        return true;
    }

    @Override
    public boolean deleteToken() {
        return FallbackSecretProvider.STATE.reset();
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}
