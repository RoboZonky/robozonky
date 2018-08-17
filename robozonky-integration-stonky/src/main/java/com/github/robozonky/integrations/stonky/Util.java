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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.internal.api.Defaults;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Util {

    private static final String APPLICATION_NAME = Defaults.ROBOZONKY_USER_AGENT;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String CREDENTIALS_FOLDER = "google"; // Directory to store user credentials.
    private static final Logger LOGGER = LoggerFactory.getLogger(Util.class);

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved credentials/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS,
                                                             SheetsScopes.DRIVE_FILE,
                                                             SheetsScopes.DRIVE);

    private static GoogleAuthorizationCodeFlow createFlow(final HttpTransport httpTransport,
                                                          final GoogleClientSecrets clientSecrets) throws IOException {
        return new GoogleAuthorizationCodeFlow.Builder(httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(CREDENTIALS_FOLDER)))
                .setAccessType("offline")
                .build();
    }

    /**
     * Creates an authorized Credential object.
     * @param sessionInfo User for which these credentials are being obtained.
     * @param httpTransport The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If there is no client_secret.
     */
    public static Credential getCredential(final SessionInfo sessionInfo, final HttpTransport httpTransport)
            throws IOException, GeneralSecurityException {
        final GoogleClientSecrets clientSecrets = createClientSecrets();
        final GoogleAuthorizationCodeFlow flow = createFlow(httpTransport, clientSecrets);
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                .authorize(sessionInfo.getUsername());
    }

    private static GoogleClientSecrets createClientSecrets() throws IOException, GeneralSecurityException {
        try (final InputStream in = new ByteArrayInputStream(ApiKey.get())) {
            return GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));
        }
    }

    public static boolean hasCredential(final SessionInfo sessionInfo, final HttpTransport transport) {
        try {
            final GoogleClientSecrets secrets = createClientSecrets();
            final GoogleAuthorizationCodeFlow flow = createFlow(transport, secrets);
            return (flow.loadCredential(sessionInfo.getUsername()) != null);
        } catch (final Exception ex) {
            LOGGER.debug("Failed retrieving user credentials.", ex);
            return false;
        }
    }

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
