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
    private final CredentialProvider credentialProvider;
    @Parameter(names = {"-u", "--username"}, description = "Zonky username.", required = true)
    private String username = null;

    GoogleCredentialsFeature(final String username, final HttpTransport transport,
                             final CredentialProvider credentialProvider) {
        this.username = username;
        this.transport = transport;
        this.credentialProvider = credentialProvider;
    }

    GoogleCredentialsFeature(final HttpTransport transport) {
        this("", transport, CredentialProvider.live(transport));
    }

    public GoogleCredentialsFeature() {
        this(Util.createTransport());
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    public void runGoogleCredentialCheck() {
        final SessionInfo sessionInfo = new SessionInfo(username);
        final Credential credential = credentialProvider.getCredential(sessionInfo);
        Util.createSheetsService(credential, transport);
        Util.createDriveService(credential, transport);
    }

    @Override
    public void setup() throws SetupFailedException {
        LOGGER.info("A web browser window may open, or you may be asked to visit a Google link.");
        LOGGER.info("Unless you allow RoboZonky to access your Google Sheets, Stonky integration will be disabled.");
        try {
            runGoogleCredentialCheck();
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
            final Credential credential = credentialProvider.getCredential(sessionInfo);
            final Drive service = Util.createDriveService(credential, transport);
            final DriveOverview driveOverview = DriveOverview.create(new SessionInfo(username), service);
            LOGGER.debug("Google Drive contents: {}.", driveOverview);
        } catch (final Exception ex) {
            throw new TestFailedException(ex);
        }
    }
}
