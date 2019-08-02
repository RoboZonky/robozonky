/*
 * Copyright 2019 The RoboZonky Project
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

import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.integrations.stonky.CredentialProvider;
import com.github.robozonky.integrations.stonky.DriveOverview;
import com.github.robozonky.integrations.stonky.Util;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.sheets.v4.Sheets;
import io.vavr.Lazy;
import picocli.CommandLine;

@CommandLine.Command(name = "google-sheets-credentials", description = GoogleCredentialsFeature.DESCRIPTION)
public final class GoogleCredentialsFeature extends AbstractFeature {

    static final String DESCRIPTION = "Obtain authorization for RoboZonky to access Google Sheets.";
    private final HttpTransport transport;
    private final Lazy<CredentialProvider> credentialProvider;
    @CommandLine.Option(names = {"-u", "--username"}, description = "Zonky username.", required = true)
    private String username;
    @CommandLine.Option(names = {"-h", "--callback-host"}, description = "Host to listen for OAuth response from " +
            "Google.")
    private String host = "localhost";
    @CommandLine.Option(names = {"-p", "--callback-port"},
            description = "Port on the host to listen for OAuth response from Google. 0 will auto-detect a free one.")
    private int port = 0;

    GoogleCredentialsFeature(final String username, final HttpTransport transport,
                             final CredentialProvider credentialProvider) {
        this.username = username;
        this.transport = transport;
        this.credentialProvider = Lazy.of(() -> credentialProvider == null ?
                CredentialProvider.live(transport, host, port) :
                credentialProvider);
    }

    private GoogleCredentialsFeature(final String username, final HttpTransport transport) {
        this(username, transport, null);
    }

    private GoogleCredentialsFeature(final HttpTransport transport) {
        this("", transport);
    }

    GoogleCredentialsFeature() { // for Picocli
        this(Util.createTransport());
    }

    public GoogleCredentialsFeature(final String username, final String host, final int port) {
        this(username, Util.createTransport());
        this.host = host;
        this.port = port;
    }

    @Override
    public String describe() {
        return DESCRIPTION;
    }

    private <T> T runGoogleCredentialCheck(final SessionInfo sessionInfo, final Function<Credential, T> provider) {
        logger.debug("Running credential check.");
        final Credential credential = credentialProvider.get().getCredential(sessionInfo);
        return provider.apply(credential);
    }

    Drive runGoogleCredentialCheckForDrive(final SessionInfo sessionInfo) {
        return runGoogleCredentialCheck(sessionInfo, c -> Util.createDriveService(c, transport));
    }

    Sheets runGoogleCredentialCheckForSheets(final SessionInfo sessionInfo) {
        return runGoogleCredentialCheck(sessionInfo, c -> Util.createSheetsService(c, transport));
    }

    private void runGoogleCredentialCheck(final SessionInfo sessionInfo) {
        runGoogleCredentialCheckForDrive(sessionInfo);
        runGoogleCredentialCheckForSheets(sessionInfo);
    }

    public void runGoogleCredentialCheck() {
        runGoogleCredentialCheck(new SessionInfo(username));
    }

    @Override
    public void setup() throws SetupFailedException {
        logger.info("A web browser window may open, or you may be asked to visit a Google link.");
        logger.info("Unless you allow RoboZonky to access your Google Sheets, Stonky integration will be disabled.");
        try {
            runGoogleCredentialCheck();
            logger.info("Press Enter to confirm that you have granted permission, otherwise exit.");
            System.in.read();
        } catch (final Exception ex) {
            throw new SetupFailedException(ex);
        }
    }

    @Override
    public void test() throws TestFailedException {
        try {
            final SessionInfo sessionInfo = new SessionInfo(username);
            final Drive service = runGoogleCredentialCheckForDrive(sessionInfo);
            final Sheets service2 = runGoogleCredentialCheckForSheets(sessionInfo);
            final DriveOverview driveOverview = DriveOverview.create(sessionInfo, service, service2);
            logger.debug("Google Drive contents: {}.", driveOverview);
        } catch (final Exception ex) {
            throw new TestFailedException(ex);
        }
    }
}
