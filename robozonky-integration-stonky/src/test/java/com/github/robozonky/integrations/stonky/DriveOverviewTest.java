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

package com.github.robozonky.integrations.stonky;

import java.io.IOException;
import java.util.UUID;

import com.github.robozonky.api.SessionInfo;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.testing.auth.oauth2.MockGoogleCredential;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class DriveOverviewTest {

    private static final SessionInfo SESSION_INFO = new SessionInfo("someone@somewhere.cz");

    private final Credential credential = new MockGoogleCredential.Builder().build();

    @Test
    void folderName() {
        assertThat(DriveOverview.getFolderName(SESSION_INFO)).isNotEmpty();
    }

    @Test
    void emptyGoogleDrive() throws IOException {
        final MultiRequestMockHttpTransport transport = new MultiRequestMockHttpTransport();
        transport.addReponseHandler(new FilesInFolderResponseHandler("root"));
        final Drive service = Util.createDriveService(credential, transport);
        final Sheets service2 = Util.createSheetsService(credential, transport);
        final DriveOverview overview = DriveOverview.create(SESSION_INFO, service, service2);
        assertSoftly(softly -> { // nothing was filled in
            softly.assertThat(overview.getFolder()).isNull();
            softly.assertThat(overview.getWallet()).isNull();
            softly.assertThat(overview.getPeople()).isNull();
        });
    }

    @Test
    void googleDriveWithoutStonkyFolder() throws IOException {
        final File randomFolder = GoogleUtil.getFolder(UUID.randomUUID().toString());
        final File randomFile = GoogleUtil.getFile(UUID.randomUUID().toString());
        final MultiRequestMockHttpTransport transport = new MultiRequestMockHttpTransport();
        transport.addReponseHandler(new FilesInFolderResponseHandler("root", randomFile, randomFolder));
        final Drive service = Util.createDriveService(credential, transport);
        final Sheets service2 = Util.createSheetsService(credential, transport);
        final DriveOverview overview = DriveOverview.create(SESSION_INFO, service, service2);
        assertSoftly(softly -> { // nothing was filled in
            softly.assertThat(overview.getFolder()).isNull();
            softly.assertThat(overview.getWallet()).isNull();
            softly.assertThat(overview.getPeople()).isNull();
        });
    }

    @Nested
    class StonkyFolderExists {

        private final File stonkyFolder = GoogleUtil.getFolder(DriveOverview.getFolderName(SESSION_INFO));
        private MultiRequestMockHttpTransport transport;
        private Drive drive;
        private Sheets sheets;
        private FilesInFolderResponseHandler stonkyFolderContent;

        @BeforeEach
        void prepareFolder() {
            transport = new MultiRequestMockHttpTransport();
            transport.addReponseHandler(new FilesInFolderResponseHandler("root", stonkyFolder));
            stonkyFolderContent = new FilesInFolderResponseHandler(stonkyFolder.getId());
            transport.addReponseHandler(stonkyFolderContent);
            drive = Util.createDriveService(credential, transport);
            sheets = Util.createSheetsService(credential, transport);
        }

        @Test
        void isEmpty() throws IOException {
            final DriveOverview overview = DriveOverview.create(SESSION_INFO, drive, sheets);
            assertSoftly(softly -> { // nothing was filled in
                softly.assertThat(overview.getFolder()).isEqualTo(stonkyFolder);
                softly.assertThat(overview.getWallet()).isNull();
                softly.assertThat(overview.getPeople()).isNull();
            });
        }

        @Test
        void hasPeopleSpreadsheet() throws IOException {
            final File peopleSpreadsheet = GoogleUtil.getSpreadsheetFile(DriveOverview.ROBOZONKY_PEOPLE_SHEET_NAME);
            stonkyFolderContent.add(peopleSpreadsheet);
            final DriveOverview overview = DriveOverview.create(SESSION_INFO, drive, sheets);
            assertSoftly(softly -> { // only one sheet is filled in
                softly.assertThat(overview.getFolder()).isEqualTo(stonkyFolder);
                softly.assertThat(overview.getWallet()).isNull();
                softly.assertThat(overview.getPeople()).isEqualTo(peopleSpreadsheet);
            });
        }

        @Nested
        class WalletSpreadsheetExists {

            private final File walletSpreadsheet =
                    GoogleUtil.getSpreadsheetFile(DriveOverview.ROBOZONKY_WALLET_SHEET_NAME);

            @BeforeEach
            void prepareFolder() {
                stonkyFolderContent.add(walletSpreadsheet);
                transport.addReponseHandler(new GetFileResponseHandler(walletSpreadsheet));
                transport.addReponseHandler(new ModifyFileResponseHandler(walletSpreadsheet));
            }

            @Test
            void withoutPeopleSpreadsheet() throws IOException {
                final DriveOverview overview = DriveOverview.create(SESSION_INFO, drive, sheets);
                assertSoftly(softly -> { // only one sheet is filled in
                    softly.assertThat(overview.getFolder()).isEqualTo(stonkyFolder);
                    softly.assertThat(overview.getWallet()).isEqualTo(walletSpreadsheet);
                    softly.assertThat(overview.getPeople()).isNull();
                });
            }

            @Nested
            class PeopleSpreadsheetExistsToo {

                private final File peopleSpreadsheet =
                        GoogleUtil.getSpreadsheetFile(DriveOverview.ROBOZONKY_PEOPLE_SHEET_NAME);

                @BeforeEach
                void prepareFolder() {
                    stonkyFolderContent.add(peopleSpreadsheet);
                    transport.addReponseHandler(new GetFileResponseHandler(peopleSpreadsheet));
                    transport.addReponseHandler(new ModifyFileResponseHandler(peopleSpreadsheet));
                }

                @Test
                void everythingIsReturned() throws IOException {
                    final DriveOverview overview = DriveOverview.create(SESSION_INFO, drive, sheets);
                    assertSoftly(softly -> { // both sheets are filled in
                        softly.assertThat(overview.getFolder()).isEqualTo(stonkyFolder);
                        softly.assertThat(overview.getWallet()).isEqualTo(walletSpreadsheet);
                        softly.assertThat(overview.getPeople()).isEqualTo(peopleSpreadsheet);
                    });
                }

                @Test
                void createStonkySpreadsheet() throws IOException {
                    final DriveOverview overview = DriveOverview.create(SESSION_INFO, drive, sheets);
                    final File stonkyMaster = GoogleUtil.getSpreadsheetFile("Some Stonky file", "someRandomId");
                    transport.addReponseHandler(new GetLatestStonkyVersionResponseHandler(stonkyMaster.getId()));
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
                    final DriveOverview overview = DriveOverview.create(SESSION_INFO, drive, sheets);
                    // copy the spreadsheet since it does not exist
                    final File result = overview.latestWallet(GoogleUtil.getDownloaded());
                    assertThat(result.getId()).isEqualTo(walletSpreadsheet.getId());
                }

                @Test
                void updatePeopleSpreadsheet() throws IOException {
                    final DriveOverview overview = DriveOverview.create(SESSION_INFO, drive, sheets);
                    // copy the spreadsheet since it does not exist
                    final File result = overview.latestPeople(GoogleUtil.getDownloaded());
                    assertThat(result.getId()).isEqualTo(peopleSpreadsheet.getId());
                }

            }
        }
    }
}
