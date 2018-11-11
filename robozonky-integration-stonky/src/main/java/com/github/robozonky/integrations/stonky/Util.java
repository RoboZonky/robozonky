/*
 * Copyright 2018 The RoboZonky Project
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

package com.github.robozonky.integrations.stonky;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.util.IoUtil;
import com.github.robozonky.util.ThrowingFunction;
import com.github.robozonky.util.ThrowingSupplier;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Util {

    static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);
    private static final String APPLICATION_NAME = Defaults.ROBOZONKY_USER_AGENT;

    private Util() {
        // no instances
    }

    public static com.google.api.services.drive.model.File getFile(final Drive driveService,
                                                                   final String id) throws IOException {
        LOGGER.debug("Loading file: {}.", id);
        return driveService.files().get(id)
                .setFields("id,name,modifiedTime")
                .execute();
    }

    public static com.google.api.services.drive.model.File copyFile(final Drive driveService,
                                                                    final com.google.api.services.drive.model.File upstream,
                                                                    final com.google.api.services.drive.model.File parent,
                                                                    final String name) throws IOException {
        LOGGER.debug("Cloning master spreadsheet '{}', setting name: {}.", upstream.getId(), name);
        final com.google.api.services.drive.model.File f = new com.google.api.services.drive.model.File();
        f.setName(name);
        f.setParents(Collections.singletonList(parent.getId()));
        final com.google.api.services.drive.model.File result = driveService.files().copy(upstream.getId(), f)
                .setFields("id,name,modifiedTime")
                .execute();
        LOGGER.debug("Created a copy: {}.", result.getId());
        return result;
    }

    public static HttpTransport createTransport() {
        try {
            return GoogleNetHttpTransport.newTrustedTransport();
        } catch (final Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Drive createDriveService(final Credential credential, final HttpTransport transport) {
        return new Drive.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static Sheets createSheetsService(final Credential credential, final HttpTransport transport) {
        return new Sheets.Builder(transport, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    static <S, T> Function<S, T> wrap(final ThrowingFunction<S, T> function) {
        return s -> {
            try {
                return function.apply(s);
            } catch (final Exception ex) {
                throw new IllegalStateException("Function failed.", ex);
            }
        };
    }

    static <T> Supplier<T> wrap(final ThrowingSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (final Exception ex) {
                throw new IllegalStateException("Supplier failed.", ex);
            }
        };
    }

    static Optional<File> download(final URL url) {
        LOGGER.debug("Will download file from {}.", url);
        try {
            return IoUtil.tryFunction(url::openStream, Util::download);
        } catch (final Exception ex) {
            LOGGER.warn("Failed downloading file.", ex);
            return Optional.empty();
        }
    }

    static Optional<File> download(final InputStream stream) {
        try {
            final File f = File.createTempFile("robozonky-", ".download");
            FileUtils.copyInputStreamToFile(stream, f);
            return Optional.of(f);
        } catch (final Exception ex) {
            LOGGER.warn("Failed downloading file.", ex);
            return Optional.empty();
        }
    }

}
