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

package com.github.robozonky.internal.secrets;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Plain-text in-memory ephemeral secret storage. Should only ever be used for testing purposes.
 */
final class FallbackSecretProvider implements SecretProvider {

    private final String username;
    private final char[] password;
    private final Map<String, char[]> secrets = new HashMap<>(0);

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
    public Optional<char[]> getSecret(final String secretId) {
        return Optional.ofNullable(this.secrets.get(secretId));
    }

    @Override
    public boolean setSecret(final String secretId, final char... secret) {
        this.secrets.put(secretId, secret);
        return true;
    }

    @Override
    public boolean isPersistent() {
        return false;
    }
}
