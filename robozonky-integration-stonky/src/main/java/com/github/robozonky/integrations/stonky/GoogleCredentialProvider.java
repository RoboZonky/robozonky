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
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.SessionInfo;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

final class GoogleCredentialProvider implements CredentialProvider {

    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCredentialProvider.class);
    private static final String CREDENTIALS_FOLDER = "google"; // Directory to store user credentials.

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved credentials/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS,
                                                             SheetsScopes.DRIVE_FILE,
                                                             SheetsScopes.DRIVE);
    private final HttpTransport transport;
    private final Supplier<Optional<byte[]>> secrets;

    GoogleCredentialProvider(final HttpTransport transport) {
        this(transport, ApiKey::get);
    }

    GoogleCredentialProvider(final HttpTransport transport, final Supplier<Optional<byte[]>> secret) {
        this.transport = transport;
        this.secrets = secret;
    }

    private static GoogleAuthorizationCodeFlow createFlow(final HttpTransport httpTransport,
                                                          final GoogleClientSecrets clientSecrets) throws IOException {
        return new GoogleAuthorizationCodeFlow.Builder(httpTransport, Util.JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(CREDENTIALS_FOLDER)))
                .setAccessType("offline")
                .build();
    }

    private GoogleClientSecrets createClientSecrets() throws IOException {
        final byte[] key = secrets.get().orElseThrow(() -> new IllegalStateException("No API key."));
        try (final InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(key))) {
            return GoogleClientSecrets.load(Util.JSON_FACTORY, r);
        }
    }

    @Override
    public boolean credentialExists(final SessionInfo sessionInfo) {
        try {
            final GoogleClientSecrets secrets = createClientSecrets();
            final GoogleAuthorizationCodeFlow flow = createFlow(transport, secrets);
            return (flow.loadCredential(sessionInfo.getUsername()) != null);
        } catch (final Exception ex) {
            LOGGER.warn("Failed retrieving user credentials.", ex);
            return false;
        }
    }

    @Override
    public Optional<Credential> getCredential(final SessionInfo sessionInfo) {
        try {
            final GoogleClientSecrets clientSecrets = createClientSecrets();
            final GoogleAuthorizationCodeFlow flow = createFlow(transport, clientSecrets);
            final Credential result = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                    .authorize(sessionInfo.getUsername());
            return Optional.of(result);
        } catch (final Exception ex) {
            LOGGER.warn("Failed obtaining Google credentials.", ex);
            return Optional.empty();
        }
    }
}
