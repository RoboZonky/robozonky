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

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.app.CommandLineInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class PlainTextInformationProvider extends SensitiveInformationProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(PlainTextInformationProvider.class);
    private static final File TOKEN_FILE = new File("robozonky.token");

    private final CommandLineInterface cli;

    public PlainTextInformationProvider(final CommandLineInterface cli) {
        this.cli = cli;
    }

    @Override
    public Optional<String> getPassword() {
        return this.cli.getPassword();
    }

    @Override
    public Optional<InputStream> getToken() {
        try {
            return Optional.of(new BufferedInputStream(new FileInputStream(PlainTextInformationProvider.TOKEN_FILE)));
        } catch (final FileNotFoundException ex) {
            return Optional.empty();
        }
    }

    @Override
    public boolean setToken(final InputStream token) {
        this.setToken(); // just in case
        try (final BufferedReader reader = new BufferedReader(new InputStreamReader(token));
             final BufferedWriter writer = new BufferedWriter(new FileWriter(PlainTextInformationProvider.TOKEN_FILE))) {
            for (final String line: reader.lines().collect(Collectors.toList())) {
                writer.write(line);
                writer.newLine();
            }
            return true;
        } catch (final IOException ex) {
            PlainTextInformationProvider.LOGGER.warn("Failed writing token into storage.", ex);
            return false;
        }
    }

    @Override
    public boolean setToken() {
        return PlainTextInformationProvider.TOKEN_FILE.delete();
    }

    @Override
    public Optional<LocalDateTime> getTokenSetDate() {
        if (PlainTextInformationProvider.TOKEN_FILE.canRead()) {
            final long lastModifiedInMillis = PlainTextInformationProvider.TOKEN_FILE.lastModified();
            final Instant lastModified = Instant.ofEpochMilli(lastModifiedInMillis);
            return Optional.of(LocalDateTime.ofInstant(lastModified, ZoneId.systemDefault()));
        } else {
            return Optional.empty();
        }
    }
}
