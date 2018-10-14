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
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.internal.util.LazyInitialized;
import com.github.robozonky.internal.util.ToStringBuilder;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriveOverview {

    static final String MIME_TYPE_XLS_SPREADSHEET = "application/vnd.ms-excel";
    static final String MIME_TYPE_ODS_SPREADSHEET = "application/x-vnd.oasis.opendocument.spreadsheet";
    static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    static final String MIME_TYPE_GOOGLE_SPREADSHEET = "application/vnd.google-apps.spreadsheet";
    static final String ROBOZONKY_PEOPLE_SHEET_NAME = "Export investic";
    static final String ROBOZONKY_WALLET_SHEET_NAME = "Export peněženky";
    private static final Logger LOGGER = LoggerFactory.getLogger(DriveOverview.class);
    private final SessionInfo sessionInfo;
    private final Drive driveService;
    private final Sheets sheetService;
    private final LazyInitialized<String> toString = ToStringBuilder.createFor(this, "sessionInfo", "driveService",
                                                                               "toString");
    private volatile File folder;
    private volatile File people;
    private volatile File wallet;

    private DriveOverview(final SessionInfo sessionInfo, final Drive driveService, final Sheets sheetService) {
        this(sessionInfo, driveService, sheetService, null);
    }

    private DriveOverview(final SessionInfo sessionInfo, final Drive driveService, final Sheets sheetService,
                          final File parent) {
        this(sessionInfo, driveService, sheetService, parent, null);
    }

    private DriveOverview(final SessionInfo sessionInfo, final Drive driveService, final Sheets sheetService,
                          final File parent, final File wallet) {
        this(sessionInfo, driveService, sheetService, parent, wallet, null);
    }

    private DriveOverview(final SessionInfo sessionInfo, final Drive driveService, final Sheets sheetService,
                          final File parent, final File wallet, final File people) {
        this.sessionInfo = sessionInfo;
        this.driveService = driveService;
        this.sheetService = sheetService;
        this.folder = parent;
        this.people = people;
        this.wallet = wallet;
    }

    static String getFolderName(final SessionInfo sessionInfo) {
        return "Stonky pro účet '" + sessionInfo.getUsername() + "'";
    }

    public static DriveOverview create(final SessionInfo sessionInfo, final Drive driveService,
                                       final Sheets sheetService) throws IOException {
        final String folderName = getFolderName(sessionInfo);
        LOGGER.debug("Listing all files in the root of Google Drive, looking up folder: {}.", folderName);
        final List<File> files = getFilesInFolder(driveService, "root").collect(Collectors.toList());
        final Optional<File> result = files.stream()
                .peek(f -> LOGGER.debug("Found '{}' ({}) as {}.", f.getName(), f.getMimeType(), f.getId()))
                .filter(f -> Objects.equals(f.getMimeType(), MIME_TYPE_FOLDER))
                .filter(f -> Objects.equals(f.getName(), folderName))
                .findFirst();
        if (result.isPresent()) {
            return create(sessionInfo, driveService, sheetService, result.get());
        } else {
            return new DriveOverview(sessionInfo, driveService, sheetService);
        }
    }

    private static Stream<File> listSpreadsheets(final Stream<File> all) {
        return all.filter(f -> Objects.equals(f.getMimeType(), MIME_TYPE_GOOGLE_SPREADSHEET));
    }

    private static Optional<File> getSpreadsheetWithName(final Stream<File> all, final String name) {
        LOGGER.debug("Looking up spreadsheet with name: {}.", name);
        return listSpreadsheets(all)
                .filter(f -> Objects.equals(f.getName(), name))
                .findFirst();
    }

    private static Stream<File> getFilesInFolder(final Drive driveService, final File parent) throws IOException {
        return getFilesInFolder(driveService, parent.getId());
    }

    private static String getFields(final String... additional) {
        return Stream.concat(Stream.of("id", "name", "modifiedTime"), Stream.of(additional))
                .collect(Collectors.joining(","));
    }

    private static Stream<File> getFilesInFolder(final Drive driveService, final String parentId) throws IOException {
        LOGGER.debug("Listing files in folder {}.", parentId);
        return driveService.files().list()
                .setQ("'" + parentId + "' in parents and trashed = false")
                .setFields("nextPageToken, files(" + getFields("mimeType") + ")")
                .execute()
                .getFiles()
                .stream()
                .peek(f -> LOGGER.debug("Found '{}' ({}) as {}.", f.getName(), f.getMimeType(), f.getId()));
    }

    private static DriveOverview create(final SessionInfo sessionInfo, final Drive driveService,
                                        final Sheets sheetService, final File parent) throws IOException {
        LOGGER.debug("Looking for a wallet spreadsheet.");
        final List<File> all = getFilesInFolder(driveService, parent).collect(Collectors.toList());
        return getSpreadsheetWithName(all.stream(), ROBOZONKY_WALLET_SHEET_NAME)
                .map(f -> createWithWallet(sessionInfo, driveService, sheetService, all.stream(), parent, f))
                .orElseGet(() -> createWithoutWallet(sessionInfo, driveService, sheetService, all.stream(), parent));
    }

    private static DriveOverview createWithWallet(final SessionInfo sessionInfo, final Drive driveService,
                                                  final Sheets sheetService, final Stream<File> all, final File parent,
                                                  final File wallet) {
        LOGGER.debug("Looking for a people spreadsheet.");
        return getSpreadsheetWithName(all, ROBOZONKY_PEOPLE_SHEET_NAME)
                .map(f -> new DriveOverview(sessionInfo, driveService, sheetService, parent, wallet, f))
                .orElseGet(() -> new DriveOverview(sessionInfo, driveService, sheetService, parent, wallet, null));
    }

    private static DriveOverview createWithoutWallet(final SessionInfo sessionInfo, final Drive driveService,
                                                     final Sheets sheetService, final Stream<File> all,
                                                     final File parent) {
        LOGGER.debug("Looking for a people spreadsheet.");
        return getSpreadsheetWithName(all, ROBOZONKY_PEOPLE_SHEET_NAME)
                .map(f -> new DriveOverview(sessionInfo, driveService, sheetService, parent, null, f))
                .orElseGet(() -> new DriveOverview(sessionInfo, driveService, sheetService, parent));
    }

    File getFolder() {
        return folder;
    }

    File getPeople() {
        return people;
    }

    File getWallet() {
        return wallet;
    }

    private File createRoboZonkyFolder(final Drive driveService) throws IOException {
        final File fileMetadata = new File();
        fileMetadata.setName(getFolderName(sessionInfo));
        fileMetadata.setDescription("RoboZonky aktualizuje obsah tohoto adresáře jednou denně brzy ráno.");
        fileMetadata.setMimeType(MIME_TYPE_FOLDER);
        final File result = driveService.files().create(fileMetadata)
                .setFields(getFields())
                .execute();
        LOGGER.debug("Created a new Google folder '{}'.", result.getId());
        return result;
    }

    private File getOrCreateRoboZonkyFolder() throws IOException {
        if (folder == null) {
            LOGGER.debug("Creating a new Stonky folder.");
            folder = createRoboZonkyFolder(driveService);
        }
        return folder;
    }

    public File latestStonky() throws IOException {
        return Properties.STONKY_COPY.getValue()
                .map(Util.wrap(id -> Util.getFile(driveService, id)))
                .orElse(createOrReuseStonky());
    }

    private String autodetectLatestStonkyVersion() throws IOException {
        final ValueRange data = sheetService.spreadsheets().values()
                .get("1ipnVJ7jehwwGLTY-Oi0eHwyfyjCNCoQGOz0oO_9Pp80", "Sheet1!A2:E2")
                .execute();
        LOGGER.trace("Received '{}' from Stonky release spreadsheet.", data);
        final List<List<Object>> matrix = data.getValues();
        final String result = (String) matrix.get(0).get(4); // cell E2
        LOGGER.debug("Stonky version auto-detected to {}.", matrix.get(0).get(1)); // cell B2
        return result;
    }

    private File createOrReuseStonky() throws IOException {
        final String masterId = Properties.STONKY_MASTER.getValue()
                .orElseGet(() -> Util.wrap(this::autodetectLatestStonkyVersion).get());
        LOGGER.debug("Will look for Stonky master spreadsheet: {}.", masterId);
        final File upstream = Util.getFile(driveService, masterId);
        final File parent = getOrCreateRoboZonkyFolder();
        final String expectedName = "My " + upstream.getName(); // such as "My Stonky 0.8 [Public Beta Release]"
        final Optional<File> stonky = listSpreadsheets(getFilesInFolder(driveService, parent))
                .filter(s -> Objects.equals(s.getName(), expectedName))
                .findFirst();
        if (stonky.isPresent()) {
            final File result = stonky.get();
            LOGGER.debug("Found a copy of Stonky: {}.", result.getId());
            return result;
        } else {
            return Util.copyFile(driveService, upstream, parent, expectedName);
        }
    }

    public File latestWallet(final java.io.File download) throws IOException {
        LOGGER.debug("Processing wallet export.");
        wallet = getLatestSpreadsheet(download, ROBOZONKY_WALLET_SHEET_NAME, wallet);
        return wallet;
    }

    public File latestPeople(final java.io.File download) throws IOException {
        LOGGER.debug("Processing investment export.");
        people = getLatestSpreadsheet(download, ROBOZONKY_PEOPLE_SHEET_NAME, people);
        return people;
    }

    public File latestWelcome(final java.io.File download) throws IOException {
        LOGGER.debug("Processing Welcome spreadsheet.");
        return createSpreadsheetFromOds(InternalSheet.WELCOME.getId(), download);
    }

    private File createSpreadsheet(final String name, final java.io.File export, final String mime) throws IOException {
        final FileContent fc = new FileContent(mime, export);
        final File parent = getOrCreateRoboZonkyFolder(); // retrieve Google folder in which to place the spreadsheet
        // convert the spreadsheet to Google Spreadsheet
        final File f = new File();
        f.setName(name);
        f.setParents(Collections.singletonList(parent.getId()));
        f.setMimeType(MIME_TYPE_GOOGLE_SPREADSHEET);
        LOGGER.debug("Creating a new Google spreadsheet: {}.", f);
        final File result = driveService.files().create(f, fc)
                .setFields(getFields())
                .execute();
        // and mark the time when the file was last updated
        LOGGER.debug("New Google spreadsheet created: {}.", result.getId());
        return result;
    }

    private File createSpreadsheetFromXls(final String name, final java.io.File export) throws IOException {
        return createSpreadsheet(name, export, MIME_TYPE_XLS_SPREADSHEET);
    }

    private File createSpreadsheetFromOds(final String name, final java.io.File export) throws IOException {
        return createSpreadsheet(name, export, MIME_TYPE_ODS_SPREADSHEET);
    }

    private File actuallyModifySpreadsheet(final File original, final java.io.File export) throws IOException {
        final FileContent fc = new FileContent(MIME_TYPE_XLS_SPREADSHEET, export);
        return driveService.files().update(original.getId(), null, fc)
                .setFields(getFields())
                .execute();
    }

    private File modifySpreadsheet(final File original, final java.io.File export) throws IOException {
        final String id = original.getId();
        LOGGER.debug("Updating an existing Google spreadsheet: {}.", id);
        final File result = actuallyModifySpreadsheet(original, export);
        LOGGER.debug("Google spreadsheet updated.");
        return result;
    }

    /**
     * Download the spreadsheet from Zonky and convert it to a Google Spreadsheet.
     * @param download File to convert.
     * @param name Name of the Google spreadsheet file to create if it doesn't exist.
     * @param original Google spreadsheet file to update, or null if none exists.
     * @return Guaranteed latest version of the Google Spreadsheet matching the Zonky spreadsheet.
     * @throws IOException
     */
    private File getLatestSpreadsheet(final java.io.File download, final String name,
                                      final File original) throws IOException {
        return (original == null) ? createSpreadsheetFromXls(name, download) : modifySpreadsheet(original, download);
    }

    @Override
    public String toString() {
        return toString.get();
    }
}
