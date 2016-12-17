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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import com.github.triceo.robozonky.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plain-text secret storage. Should only be used as fallback in case the JDK does not support KeyStores. This is
 * unlikely, but we've seen that happen. (See https://github.com/triceo/robozonky/issues/52.)
 */
final class FallbackSecretProvider extends SecretProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(FallbackSecretProvider.class);
    static final File TOKEN = new File("robozonky.token");

    private final String username;
    private final char[] password;
    private final Map<String, char[]> secrets = new HashMap<>();

    public FallbackSecretProvider(final String username, final char[] password) {
        this.username = username;
        this.password = password;
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
        try (final BufferedReader br =
                     Files.newBufferedReader(FallbackSecretProvider.TOKEN.toPath(), Defaults.CHARSET)){
            final String result = br.readLine();
            return (result == null) ? Optional.empty() : Optional.of(new StringReader(result));
        } catch (final IOException ex) {
            FallbackSecretProvider.LOGGER.warn("Failed obtaining token.", ex);
            return Optional.empty();
        }
    }

    @Override
    public boolean setToken(final Reader token) {
        try (final BufferedReader r = new BufferedReader(token); final BufferedWriter bw =
                Files.newBufferedWriter(FallbackSecretProvider.TOKEN.toPath(), Defaults.CHARSET,
                        StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            bw.write(r.readLine());
            bw.newLine();
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
        return FallbackSecretProvider.TOKEN.delete();
    }

    @Override
    public Optional<OffsetDateTime> getTokenSetDate() {
        if (FallbackSecretProvider.TOKEN.exists()) {
            final long millisSinceEpoch = FallbackSecretProvider.TOKEN.lastModified();
            final Instant then = Instant.EPOCH.plus(millisSinceEpoch, ChronoUnit.MILLIS);
            return Optional.of(OffsetDateTime.ofInstant(then, Defaults.ZONE_ID));
        } else {
            return Optional.empty();
        }
    }
}
