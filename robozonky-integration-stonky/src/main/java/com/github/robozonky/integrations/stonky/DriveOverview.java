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
import java.util.function.Supplier;
import java.util.stream.Stream;

import com.github.robozonky.api.SessionInfo;
import com.google.api.client.http.FileContent;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriveOverview {

    static final String MIME_TYPE_XLS_SPREADSHEET = "application/vnd.ms-excel";
    static final String MIME_TYPE_FOLDER = "application/vnd.google-apps.folder";
    static final String MIME_TYPE_GOOGLE_SPREADSHEET = "application/vnd.google-apps.spreadsheet";
    static final String ROBOZONKY_PEOPLE_SHEET_NAME = "Export investic";
    static final String ROBOZONKY_WALLET_SHEET_NAME = "Export peněženky";
    private static final Logger LOGGER = LoggerFactory.getLogger(DriveOverview.class);
    private final SessionInfo sessionInfo;
    private final Drive driveService;
    private volatile File folder;
    private volatile File people;
    private volatile File wallet;

    private DriveOverview(final SessionInfo sessionInfo, final Drive driveService) {
        this(sessionInfo, driveService, null);
    }

    private DriveOverview(final SessionInfo sessionInfo, final Drive driveService, final File parent) {
        this(sessionInfo, driveService, parent, null);
    }

    private DriveOverview(final SessionInfo sessionInfo, final Drive driveService, final File parent,
                          final File wallet) {
        this(sessionInfo, driveService, parent, wallet, null);
    }

    private DriveOverview(final SessionInfo sessionInfo, final Drive driveService, final File parent, final File wallet,
                          final File people) {
        this.sessionInfo = sessionInfo;
        this.driveService = driveService;
        this.folder = parent;
        this.people = people;
        this.wallet = wallet;
    }

    static String getFolderName(final SessionInfo sessionInfo) {
        return "Stonky pro účet '" + sessionInfo.getUsername() + "'";
    }

    public static DriveOverview create(final SessionInfo sessionInfo, final Drive driveService) throws IOException {
        final String folderName = getFolderName(sessionInfo);
        LOGGER.debug("Listing all files in the Google Drive, looking up folder: {}.", folderName);
        final List<File> files = driveService.files().list().execute().getFiles();
        final Optional<File> result = files.stream()
                .peek(f -> LOGGER.debug("Found '{}' ({}) as {}.", f.getName(), f.getMimeType(), f.getId()))
                .filter(f -> Objects.equals(f.getMimeType(), MIME_TYPE_FOLDER))
                .filter(f -> Objects.equals(f.getName(), folderName))
                .findFirst();
        if (result.isPresent()) {
            return create(sessionInfo, driveService, result.get());
        } else {
            return new DriveOverview(sessionInfo, driveService);
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
        LOGGER.debug("Listing files in folder {}.", parent.getId());
        return driveService.files().list()
                .setQ("'" + parent.getId() + "' in parents")
                .execute()
                .getFiles()
                .stream()
                .peek(f -> LOGGER.debug("Found '{}' ({}) as {}.", f.getName(), f.getMimeType(), f.getId()));
    }

    private static DriveOverview create(final SessionInfo sessionInfo, final Drive driveService,
                                        final File parent) throws IOException {
        LOGGER.debug("Looking for a wallet spreadsheet.");
        final Stream<File> all = getFilesInFolder(driveService, parent);
        return getSpreadsheetWithName(all, ROBOZONKY_WALLET_SHEET_NAME)
                .map(f -> createWithWallet(sessionInfo, driveService, all, parent, f))
                .orElseGet(() -> createWithoutWallet(sessionInfo, driveService, all, parent));
    }

    private static DriveOverview createWithWallet(final SessionInfo sessionInfo, final Drive driveService,
                                                  final Stream<File> all, final File parent, final File wallet) {
        LOGGER.debug("Looking for a people spreadsheet.");
        return getSpreadsheetWithName(all, ROBOZONKY_PEOPLE_SHEET_NAME)
                .map(f -> new DriveOverview(sessionInfo, driveService, parent, wallet, f))
                .orElseGet(() -> new DriveOverview(sessionInfo, driveService, parent, wallet, null));
    }

    private static DriveOverview createWithoutWallet(final SessionInfo sessionInfo, final Drive driveService,
                                                     final Stream<File> all, final File parent) {
        LOGGER.debug("Looking for a people spreadsheet.");
        return getSpreadsheetWithName(all, ROBOZONKY_PEOPLE_SHEET_NAME)
                .map(f -> new DriveOverview(sessionInfo, driveService, parent, null, f))
                .orElseGet(() -> new DriveOverview(sessionInfo, driveService, parent));
    }

    private static String identify(final File file) {
        return file == null ? null : file.getId();
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
        fileMetadata.setDescription("RoboZonky aktualizuje obsah tohoto adresáře jednou denně brzy ráno." +
                                            "Adresář ani jeho obsah nemažte a pokud už to udělat musíte, " +
                                            "nezapomeňte ho odstranit také z Koše.");
        fileMetadata.setMimeType(MIME_TYPE_FOLDER);
        final File result = driveService.files().create(fileMetadata)
                .setFields("id")
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

    private File cloneStonky(final File upstream, final File parent) throws IOException {
        LOGGER.debug("Cloning Stonky master spreadsheet.");
        final File f = new File();
        f.setName(upstream.getName());
        f.setParents(Collections.singletonList(parent.getId()));
        final File result = driveService.files().copy(upstream.getId(), f)
                .setFields("id")
                .execute();
        LOGGER.debug("Created a copy of Stonky: {}.", result.getId());
        return result;
    }

    public File latestStonky() throws IOException {
        return Properties.STONKY_COPY.getValue()
                .map(Util.wrap(this::getStonkyOrFail))
                .orElse(createOrReuseStonky());
    }

    private File getStonkyOrFail(final String fileId) throws IOException {
        LOGGER.debug("Will look for Stonky spreadsheet: {}.", fileId);
        return driveService.files().get(fileId).execute();
    }

    private File createOrReuseStonky() throws IOException {
        final String masterId =
                Properties.STONKY_MASTER.getValue().orElseThrow(() -> new IllegalStateException("Impossible"));
        LOGGER.debug("Will look for Stonky master spreadsheet: {}.", masterId);
        final File upstream = driveService.files().get(masterId).execute();
        final File parent = getOrCreateRoboZonkyFolder();
        final Optional<File> stonky = listSpreadsheets(getFilesInFolder(driveService, parent))
                .filter(s -> Objects.equals(s.getName(), upstream.getName()))
                .findFirst();
        if (stonky.isPresent()) {
            final File result = stonky.get();
            LOGGER.debug("Found a copy of Stonky: {}.", result.getId());
            return result;
        } else {
            return cloneStonky(upstream, parent);
        }
    }

    public File latestWallet(final Supplier<java.io.File> downloader) throws IOException {
        LOGGER.debug("Processing wallet export.");
        wallet = getLatestSpreadsheet(downloader, ROBOZONKY_WALLET_SHEET_NAME, wallet);
        return wallet;
    }

    public File latestPeople(final Supplier<java.io.File> downloader) throws IOException {
        LOGGER.debug("Processing investment export.");
        people = getLatestSpreadsheet(downloader, ROBOZONKY_PEOPLE_SHEET_NAME, people);
        return people;
    }

    private File createSpreadsheet(final String name, final Supplier<java.io.File> downloader) throws IOException {
        final java.io.File export = downloader.get(); // download from Zonky first
        final FileContent fc = new FileContent(MIME_TYPE_XLS_SPREADSHEET, export);
        final File parent = getOrCreateRoboZonkyFolder(); // retrieve Google folder in which to place the spreadsheet
        // convert the Zonky XLS to Google Spreadsheet
        final File f = new File();
        f.setName(name);
        f.setParents(Collections.singletonList(parent.getId()));
        f.setMimeType(MIME_TYPE_GOOGLE_SPREADSHEET);
        LOGGER.debug("Creating a new Google spreadsheet: {}.", f);
        final File result = driveService.files().create(f, fc)
                .setFields("id")
                .execute();
        // and mark the time when the file was last updated
        LOGGER.debug("New Google spreadsheet created: {}.", result.getId());
        return result;
    }

    private File actuallyModifySpreadsheet(final File original,
                                           final Supplier<java.io.File> downloader) throws IOException {
        final java.io.File export = downloader.get();
        final FileContent fc = new FileContent(MIME_TYPE_XLS_SPREADSHEET, export);
        return driveService.files().update(original.getId(), null, fc)
                .setFields("id")
                .execute();
    }

    private File modifySpreadsheet(final File original, final Supplier<java.io.File> downloader) throws IOException {
        final String id = identify(original);
        LOGGER.debug("Updating an existing Google spreadsheet: {}.", id);
        final File result = actuallyModifySpreadsheet(original, downloader);
        LOGGER.debug("Google spreadsheet updated.");
        return result;
    }

    /**
     * Download the spreadsheet from Zonky and convert it to a Google Spreadsheet.
     * @param downloader Called to download the file.
     * @param name Name of the Google spreadsheet file to create if it doesn't exist.
     * @param original Google spreadsheet file to update, or null if none exists.
     * @return Guaranteed latest version of the Google Spreadsheet matching the Zonky spreadsheet.
     * @throws IOException
     */
    private File getLatestSpreadsheet(final Supplier<java.io.File> downloader, final String name,
                                      final File original) throws IOException {
        return (original == null) ? createSpreadsheet(name, downloader) : modifySpreadsheet(original, downloader);
    }

    @Override
    public String toString() {
        return "DriveOverview{" +
                "folder=" + identify(folder) +
                ", people=" + identify(people) +
                ", wallet=" + identify(wallet) +
                '}';
    }
}
