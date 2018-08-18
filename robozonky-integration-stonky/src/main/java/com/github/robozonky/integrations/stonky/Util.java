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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.internal.api.Defaults;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;

public class Util {

    private static final String APPLICATION_NAME = Defaults.ROBOZONKY_USER_AGENT;
    static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

    public static HttpTransport createTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
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

    public static <S, T> Function<S, T> wrap(final ThrowingFunction<S, T> function) {
        return (s) -> {
            try {
                return function.apply(s);
            } catch (final Exception ex) {
                throw new IllegalStateException("Function failed.", ex);
            }
        };
    }

    public static <T> Supplier<T> wrap(final ThrowingSupplier<T> supplier) {
        return () -> {
            try {
                return supplier.get();
            } catch (final Exception ex) {
                throw new IllegalStateException("Supplier failed.", ex);
            }
        };
    }

    interface ThrowingFunction<S, T> {

        T apply(S argument) throws Exception;
    }

    interface ThrowingSupplier<T> {

        T get() throws Exception;
    }
}
