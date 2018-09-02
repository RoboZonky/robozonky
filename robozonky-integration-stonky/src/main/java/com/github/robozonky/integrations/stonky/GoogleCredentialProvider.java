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
import com.github.robozonky.util.IoUtil;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.java6.auth.oauth2.VerificationCodeReceiver;
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
    private static final String CREDENTIALS_FOLDER = "Google"; // Directory to store user credentials.

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved credentials/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(SheetsScopes.SPREADSHEETS,
                                                             SheetsScopes.DRIVE_FILE,
                                                             SheetsScopes.DRIVE);
    private final HttpTransport transport;
    private final String host;
    private final int port;
    private final Supplier<byte[]> secrets;

    GoogleCredentialProvider(final HttpTransport transport, final String callbackHost, final int callbackPort) {
        this(transport, callbackHost, callbackPort, ApiKey::get);
    }

    GoogleCredentialProvider(final HttpTransport transport, final String callbackHost, final int callbackPort,
                             final Supplier<byte[]> secret) {
        this.transport = transport;
        this.host = callbackHost;
        this.port = callbackPort;
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
        return IoUtil.applyCloseable(() -> new ByteArrayInputStream(key),
                                     s -> GoogleClientSecrets.load(Util.JSON_FACTORY, new InputStreamReader(s)));
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
            LOGGER.debug("Will listen on {}:{}.", host, port);
            final VerificationCodeReceiver receiver = new LocalServerReceiver.Builder()
                    .setHost(host)
                    .setPort(port)
                    .build();
            final AuthorizationCodeFlow flow = createFlow(transport);
            return new AuthorizationCodeInstalledApp(flow, receiver).authorize(sessionInfo.getUsername());
        } catch (final Exception ex) {
            throw new IllegalStateException("Google credential not found.", ex);
        }
    }
}
