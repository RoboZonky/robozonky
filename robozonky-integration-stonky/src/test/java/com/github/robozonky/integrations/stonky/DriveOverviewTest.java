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
import java.util.UUID;

import com.github.robozonky.api.SessionInfo;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class DriveOverviewTest {

    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.cz");

    private final Credential credential = new MockGoogleCredential.Builder().build();

    private File getFolder(final String name) {
        final File result = getFile(name);
        result.setMimeType(DriveOverview.MIME_TYPE_FOLDER);
        return result;
    }

    private static java.io.File getDownloaded() {
        try {
            return java.io.File.createTempFile("robozonky-", ".testing");
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    private File getFile(final String name) {
        return getFile(name, UUID.randomUUID().toString());
    }

    private File getFile(final String name, final String id) {
        final File result = new File();
        result.setId(id);
        result.setMimeType("application/vnd.google-apps.files");
        result.setName(name);
        return result;
    }

    private File getSpreadsheetFile(final String name) {
        return getSpreadsheetFile(name, UUID.randomUUID().toString());
    }

    private File getSpreadsheetFile(final String name, final String id) {
        final File result = getFile(name, id);
        result.setMimeType(DriveOverview.MIME_TYPE_GOOGLE_SPREADSHEET);
        return result;
    }

    @Test
    void emptyGoogleDrive() throws IOException {
        final MultiRequestMockHttpTransport transport = new MultiRequestMockHttpTransport();
        transport.addReponseHandler(new AllFilesResponseHandler());
        final Drive service = Util.createDriveService(credential, transport);
        final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
        assertSoftly(softly -> { // nothing was filled in
            softly.assertThat(overview.getFolder()).isNull();
            softly.assertThat(overview.getWallet()).isNull();
            softly.assertThat(overview.getPeople()).isNull();
        });
    }

    @Test
    void googleDriveWithoutStonkyFolder() throws IOException {
        final File randomFolder = getFolder(UUID.randomUUID().toString());
        final File randomFile = getFile(UUID.randomUUID().toString());
        final MultiRequestMockHttpTransport transport = new MultiRequestMockHttpTransport();
        transport.addReponseHandler(new AllFilesResponseHandler(randomFile, randomFolder));
        final Drive service = Util.createDriveService(credential, transport);
        final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
        assertSoftly(softly -> { // nothing was filled in
            softly.assertThat(overview.getFolder()).isNull();
            softly.assertThat(overview.getWallet()).isNull();
            softly.assertThat(overview.getPeople()).isNull();
        });
    }

    @Nested
    class StonkyFolderExists {

        private final File stonkyFolder = getFolder(DriveOverview.getFolderName(SESSION_INFO));
        private MultiRequestMockHttpTransport transport;
        private Drive service;
        private FilesInFolderResponseHandler stonkyFolderContent;

        @BeforeEach
        void prepareFolder() {
            transport = new MultiRequestMockHttpTransport();
            transport.addReponseHandler(new AllFilesResponseHandler(stonkyFolder));
            stonkyFolderContent = new FilesInFolderResponseHandler(stonkyFolder);
            transport.addReponseHandler(stonkyFolderContent);
            service = Util.createDriveService(credential, transport);
        }

        @Test
        void isEmpty() throws IOException {
            final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
            assertSoftly(softly -> { // nothing was filled in
                softly.assertThat(overview.getFolder()).isEqualTo(stonkyFolder);
                softly.assertThat(overview.getWallet()).isNull();
                softly.assertThat(overview.getPeople()).isNull();
            });
        }

        @Test
        void hasPeopleSpreadsheet() throws IOException {
            final File peopleSpreadsheet = getSpreadsheetFile(DriveOverview.ROBOZONKY_PEOPLE_SHEET_NAME);
            stonkyFolderContent.add(peopleSpreadsheet);
            final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
            assertSoftly(softly -> { // only one sheet is filled in
                softly.assertThat(overview.getFolder()).isEqualTo(stonkyFolder);
                softly.assertThat(overview.getWallet()).isNull();
                softly.assertThat(overview.getPeople()).isEqualTo(peopleSpreadsheet);
            });
        }

        @Nested
        class WalletSpreadsheetExists {

            private final File walletSpreadsheet = getSpreadsheetFile(DriveOverview.ROBOZONKY_WALLET_SHEET_NAME);

            @BeforeEach
            void prepareFolder() {
                stonkyFolderContent.add(walletSpreadsheet);
                transport.addReponseHandler(new GetFileResponseHandler(walletSpreadsheet));
                transport.addReponseHandler(new ModifyFileResponseHandler(walletSpreadsheet));
            }

            @Test
            void withoutPeopleSpreadsheet() throws IOException {
                final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
                assertSoftly(softly -> { // only one sheet is filled in
                    softly.assertThat(overview.getFolder()).isEqualTo(stonkyFolder);
                    softly.assertThat(overview.getWallet()).isEqualTo(walletSpreadsheet);
                    softly.assertThat(overview.getPeople()).isNull();
                });
            }

            @Nested
            class PeopleSpreadsheetExistsToo {

                private final File peopleSpreadsheet = getSpreadsheetFile(DriveOverview.ROBOZONKY_PEOPLE_SHEET_NAME);

                @BeforeEach
                void prepareFolder() {
                    stonkyFolderContent.add(peopleSpreadsheet);
                    transport.addReponseHandler(new GetFileResponseHandler(peopleSpreadsheet));
                    transport.addReponseHandler(new ModifyFileResponseHandler(peopleSpreadsheet));
                }

                @Test
                void everythingIsReturned() throws IOException {
                    final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
                    assertSoftly(softly -> { // both sheets are filled in
                        softly.assertThat(overview.getFolder()).isEqualTo(stonkyFolder);
                        softly.assertThat(overview.getWallet()).isEqualTo(walletSpreadsheet);
                        softly.assertThat(overview.getPeople()).isEqualTo(peopleSpreadsheet);
                    });
                }

                @Test
                void createStonkySpreadsheet() throws IOException {
                    final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
                    final String stonkySpreadsheetToCopy = Properties.STONKY_MASTER.getValue()
                            .orElseThrow(IllegalStateException::new);
                    final File stonkyMaster = getSpreadsheetFile("Some Stonky file", stonkySpreadsheetToCopy);
                    transport.addReponseHandler(new GetFileResponseHandler(stonkyMaster));
                    transport.addReponseHandler(new CopyFileResponseHandler(stonkyMaster));
                    // copy the spreadsheet since it does not exist
                    final File result = overview.latestStonky();
                    assertSoftly(softly -> {
                        softly.assertThat(result).isNotNull();
                        softly.assertThat(result.getId()).isNotEqualTo(stonkyMaster.getId());
                    });
                    stonkyFolderContent.add(result); // make mock remote folder include the file
                    transport.addReponseHandler(new GetFileResponseHandler(result)); // make mock remote api know the id
                    // we have the spreadsheet already, so the same file should be returned
                    final File result2 = overview.latestStonky();
                    assertThat(result2.getId()).isEqualTo(result.getId());
                }

                @Test
                void updateWalletSpreadsheet() throws IOException {
                    final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
                    // copy the spreadsheet since it does not exist
                    final File result = overview.latestWallet(DriveOverviewTest::getDownloaded);
                    assertThat(result.getId()).isEqualTo(walletSpreadsheet.getId());
                }

                @Test
                void updatePeopleSpreadsheet() throws IOException {
                    final DriveOverview overview = DriveOverview.create(SESSION_INFO, service);
                    // copy the spreadsheet since it does not exist
                    final File result = overview.latestPeople(DriveOverviewTest::getDownloaded);
                    assertThat(result.getId()).isEqualTo(peopleSpreadsheet.getId());
                }
            }
        }
    }
}
