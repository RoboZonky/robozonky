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

package com.github.robozonky.cli;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.integrations.stonky.CredentialProvider;
import com.github.robozonky.integrations.stonky.DriveOverview;
import com.github.robozonky.integrations.stonky.Util;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.drive.Drive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(commandNames = "google-sheets-credentials", commandDescription = GoogleCredentialsFeature.DESCRIPTION)
public final class GoogleCredentialsFeature implements Feature {

    static final String DESCRIPTION = "Obtain authorization for RoboZonky to access Google Sheets.";
    private static final Logger LOGGER = LoggerFactory.getLogger(GoogleCredentialsFeature.class);
    private final HttpTransport transport;
    private volatile CredentialProvider credentialProvider;
    @Parameter(names = {"-u", "--username"}, description = "Zonky username.", required = true)
    private String username = null;
    @Parameter(names = {"-h", "--callback-host"}, description = "Host to listen for OAuth response from Google.")
    private String host = "localhost";
    @Parameter(names = {"-p", "--callback-port"},
            description = "Port on the host to listen for OAuth response from Google. 0 will auto-detect a free one.")
    private int port = 0;

    GoogleCredentialsFeature(final String username, final HttpTransport transport,
                             final CredentialProvider credentialProvider) {
        this.username = username;
        this.transport = transport;
        this.credentialProvider = credentialProvider;
    }

    private GoogleCredentialsFeature(final HttpTransport transport) {
        this("", transport, null);
    }

    public GoogleCredentialsFeature() {
        this(Util.createTransport());
    }

    private synchronized CredentialProvider getCredentialProvider() {
        // lazy-initialized to account for host/port only having been set after the constructor calls
        if (credentialProvider == null) {
            credentialProvider = CredentialProvider.live(transport, host, port);
        }
        return credentialProvider;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    private Drive runGoogleCredentialCheck(final SessionInfo sessionInfo) {
        final Credential credential = getCredentialProvider().getCredential(sessionInfo);
        Util.createSheetsService(credential, transport);
        return Util.createDriveService(credential, transport);
    }

    public void runGoogleCredentialCheck() {
        final SessionInfo sessionInfo = new SessionInfo(username);
        runGoogleCredentialCheck(sessionInfo);
    }

    @Override
    public void setup() throws SetupFailedException {
        LOGGER.info("A web browser window may open, or you may be asked to visit a Google link.");
        LOGGER.info("Unless you allow RoboZonky to access your Google Sheets, Stonky integration will be disabled.");
        try {
            final SessionInfo sessionInfo = new SessionInfo(username);
            runGoogleCredentialCheck(sessionInfo);
            LOGGER.info("Press Enter to confirm that you have granted permission, otherwise exit.");
            System.in.read();
        } catch (final Exception ex) {
            throw new SetupFailedException(ex);
        }
    }

    @Override
    public void test() throws TestFailedException {
        try {
            final SessionInfo sessionInfo = new SessionInfo(username);
            final Drive service = runGoogleCredentialCheck(sessionInfo);
            final DriveOverview driveOverview = DriveOverview.create(new SessionInfo(username), service);
            LOGGER.debug("Google Drive contents: {}.", driveOverview);
        } catch (final Exception ex) {
            throw new TestFailedException(ex);
        }
    }
}
