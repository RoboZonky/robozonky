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
import java.util.function.Supplier;

import com.github.robozonky.api.SessionInfo;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.SheetsScopes;

final class GoogleCredentialProvider implements CredentialProvider {

    private static final String CREDENTIALS_FOLDER = "Google"; // Directory to store user credentials.

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved credentials/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS,
                                                             SheetsScopes.DRIVE_FILE,
                                                             SheetsScopes.DRIVE);
    private final HttpTransport transport;
    private final Supplier<byte[]> secrets;

    GoogleCredentialProvider(final HttpTransport transport) {
        this(transport, ApiKey::get);
    }

    GoogleCredentialProvider(final HttpTransport transport, final Supplier<byte[]> secret) {
        this.transport = transport;
        this.secrets = secret;
    }

    private AuthorizationCodeFlow createFlow(final HttpTransport httpTransport) throws IOException {
        return new GoogleAuthorizationCodeFlow.Builder(httpTransport, Util.JSON_FACTORY, createClientSecrets(), SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(CREDENTIALS_FOLDER)))
                .setAccessType("offline")
                .build();
    }

    private GoogleClientSecrets createClientSecrets() throws IOException {
        final byte[] key = secrets.get();
        final InputStreamReader r = new InputStreamReader(new ByteArrayInputStream(key));
        try { // not using try-with-resources, as PITest generates 7 untestable conditions instead of the finally block
            return GoogleClientSecrets.load(Util.JSON_FACTORY, r);
        } finally {
            r.close();
        }
    }

    @Override
    public boolean credentialExists(final SessionInfo sessionInfo) {
        try {
            final AuthorizationCodeFlow flow = createFlow(transport);
            return (flow.loadCredential(sessionInfo.getUsername()) != null);
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed reading Google credentials.", ex);
        }
    }

    @Override
    public Credential getCredential(final SessionInfo sessionInfo) {
        try {
            final AuthorizationCodeFlow flow = createFlow(transport);
            return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
                    .authorize(sessionInfo.getUsername());
        } catch (final Exception ex) {
            throw new IllegalStateException("Google credential not found.", ex);
        }
    }
}
