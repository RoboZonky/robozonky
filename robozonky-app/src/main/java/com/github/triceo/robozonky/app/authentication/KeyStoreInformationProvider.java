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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class KeyStoreInformationProvider extends SensitiveInformationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(KeyStoreInformationProvider.class);
    private static final String ALIAS_PASSWORD = "pwd";
    private static final String ALIAS_TOKEN = "tkn";
    private static final String ALIAS_TOKEN_DATE = "tknd";

    private final KeyStoreHandler ksh;

    public KeyStoreInformationProvider(final KeyStoreHandler ksh) {
        this.ksh = ksh;
    }

    @Override
    public Optional<String> getPassword() {
        return this.ksh.get(KeyStoreInformationProvider.ALIAS_PASSWORD);
    }

    @Override
    public Optional<InputStream> getToken() {
        final Optional<String> stored = this.ksh.get(KeyStoreInformationProvider.ALIAS_TOKEN);
        if (stored.isPresent()) {
            return Optional.of(new ByteArrayInputStream(stored.get().getBytes()));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean setToken(final InputStream token) {
        try {
            this.ksh.set(KeyStoreInformationProvider.ALIAS_TOKEN, token);
            this.ksh.set(KeyStoreInformationProvider.ALIAS_TOKEN_DATE, LocalDateTime.now().toString());
            return true;
        } catch (final IOException ex) {
            return false;
        }
    }

    @Override
    public boolean setToken() {
        boolean result = this.ksh.delete(KeyStoreInformationProvider.ALIAS_TOKEN);
        result = this.ksh.delete(KeyStoreInformationProvider.ALIAS_TOKEN_DATE) && result;
        return result;
    }

    @Override
    public Optional<LocalDateTime> getTokenSetDate() {
        final Optional<String> stored = this.ksh.get(KeyStoreInformationProvider.ALIAS_TOKEN_DATE);
        if (stored.isPresent()) {
            return Optional.of(LocalDateTime.parse(stored.get()));
        } else {
            return Optional.empty();
        }
    }
}
