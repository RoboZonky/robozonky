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
import java.io.InputStream;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.BiFunction;
import java.util.function.Function;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.DateUtil;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.HttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.BatchUpdateSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.CopySheetToAnotherSpreadsheetRequest;
import com.google.api.services.sheets.v4.model.DeleteSheetRequest;
import com.google.api.services.sheets.v4.model.Request;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.UpdateSheetPropertiesRequest;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

class Stonky implements Function<Tenant, Optional<String>> {

    private static final Logger LOGGER = LogManager.getLogger(Stonky.class);

    private final HttpTransport transport;
    private final CredentialProvider credentialSupplier;

    public Stonky() {
        this(Util.createTransport());
    }

    Stonky(final HttpTransport transport) {
        this(transport,
             CredentialProvider.live(transport, Properties.GOOGLE_CALLBACK_HOST.getValue().orElse("localhost"),
                                     Integer.parseInt(Properties.GOOGLE_CALLBACK_PORT.getValue().orElse("0"))));
    }

    Stonky(final HttpTransport transport, final CredentialProvider credentialSupplier) {
        this.transport = transport;
        this.credentialSupplier = credentialSupplier;
    }

    /**
     * This is synchronized because if it weren't and two copies were happening at the same time, Google API would
     * have thrown an undescribed HTTP 500 error when trying to execute the actual copying operation. A working theory
     * is that all the old sheet IDs are invalidated when a new sheet is added - but this is not verified.
     * @param sheetsService
     * @param stonky
     * @param export
     * @return
     * @throws IOException
     */
    private static synchronized SheetProperties copySheet(final Sheets sheetsService, final Spreadsheet stonky,
                                                          final File export) throws IOException {
        final int sheetId = sheetsService.spreadsheets().get(export.getId())
                .execute()
                .getSheets()
                .get(0) // first and only sheet
                .getProperties()
                .getSheetId();
        final CopySheetToAnotherSpreadsheetRequest r = new CopySheetToAnotherSpreadsheetRequest()
                .setDestinationSpreadsheetId(stonky.getSpreadsheetId());
        LOGGER.debug("Will copy sheet {} from spreadsheet '{}' to spreadsheet '{}'", sheetId, export.getId(),
                     stonky.getSpreadsheetId());
        return sheetsService.spreadsheets().sheets()
                .copyTo(export.getId(), sheetId, r)
                .execute()
                .clone();
    }

    private static Spreadsheet copySheet(final Sheets sheetsService, final Spreadsheet stonky, final File export,
                                         final InternalSheet sheet) throws IOException {
        final String name = sheet.getId();
        LOGGER.debug("Requested to copy sheet '{}' to Stonky '{}' from imported '{}'.", name, stonky.getSpreadsheetId(),
                     export.getId());
        final Optional<Sheet> targetSheet = stonky.getSheets().stream()
                .filter(s -> Objects.equals(s.getProperties().getTitle(), name))
                .findFirst();
        final List<Request> requests = new ArrayList<>(0);
        targetSheet.ifPresent(s -> {
            final int sheetId = s.getProperties().getSheetId();
            LOGGER.debug("Will delete existing '{}' sheet #{}.", name, sheetId);
            final DeleteSheetRequest delete = new DeleteSheetRequest().setSheetId(sheetId);
            requests.add(new Request().setDeleteSheet(delete));
        });
        LOGGER.debug("Copying sheet.");
        final SheetProperties newSheet = copySheet(sheetsService, stonky, export)
                .setIndex(sheet.getOrder())
                .setTitle(name);
        final UpdateSheetPropertiesRequest update = new UpdateSheetPropertiesRequest()
                .setFields("title,index")
                .setProperties(newSheet);
        requests.add(new Request().setUpdateSheetProperties(update));
        final BatchUpdateSpreadsheetRequest batch = new BatchUpdateSpreadsheetRequest()
                .setRequests(requests);
        LOGGER.debug("Renaming sheet and changing position.");
        sheetsService.spreadsheets().batchUpdate(stonky.getSpreadsheetId(), batch).execute();
        LOGGER.debug("Stonky '{}' sheet processed.", name);
        return stonky;
    }

    private String run(final Tenant tenant) throws ExecutionException, InterruptedException {
        final SessionInfo sessionInfo = tenant.getSessionInfo();
        final Credential credential = credentialSupplier.getCredential(sessionInfo);
        final Drive driveService = Util.createDriveService(credential, transport);
        final Sheets sheetsService = Util.createSheetsService(credential, transport);
        final CompletableFuture<Summary> summary = CompletableFuture.supplyAsync(Util.wrap(() -> {
            final DriveOverview o = DriveOverview.create(sessionInfo, driveService, sheetsService);
            LOGGER.debug("Google Drive overview: {}.", o);
            final File s = o.latestStonky();
            LOGGER.debug("Making a backup of the existing spreadsheet.");
            final Instant lastModified = Instant.EPOCH.plus(Duration.ofMillis(s.getModifiedTime().getValue()));
            final LocalDate d = lastModified.atZone(Defaults.ZONE_ID).toLocalDate();
            if (d.isBefore(DateUtil.localNow().toLocalDate())) {
                Util.copyFile(driveService, s, o.getFolder(), d + " " + s.getName());
            }
            final Spreadsheet result = sheetsService.spreadsheets().get(s.getId()).execute();
            return new Summary(o, result);
        }));
        final CompletableFuture<Spreadsheet> walletCopier = summary
                .thenCombineAsync(Export.WALLET.download(tenant),
                                  (s, maybeFile) -> new SummaryWithExport(s, maybeFile.orElse(null)))
                .thenApplyAsync(Util.wrap(s -> {
                    LOGGER.debug("Requesting wallet export.");
                    final File f = s.getOverview().latestWallet(s.getExport());
                    return copySheet(sheetsService, s.getStonky(), f, InternalSheet.WALLET);
                }));
        final CompletableFuture<Spreadsheet> peopleCopier = summary
                .thenCombineAsync(Export.INVESTMENTS.download(tenant),
                                  (s, maybeFile) -> new SummaryWithExport(s, maybeFile.orElse(null)))
                .thenApplyAsync(Util.wrap(s -> {
                    LOGGER.debug("Requesting investments export.");
                    final File f = s.getOverview().latestPeople(s.getExport());
                    return copySheet(sheetsService, s.getStonky(), f, InternalSheet.PEOPLE);
                }));
        final CompletableFuture<Spreadsheet> welcomeCopier = summary
                .thenApplyAsync(Util.wrap(s -> { // this is how Stonky tells the sheet was updated by RoboZonky
                    LOGGER.debug("Preparing Welcome sheet.");
                    final InputStream i = getClass().getResourceAsStream("stonky-welcome.ods");
                    final java.io.File welcome = Util.download(i).orElse(null);
                    LOGGER.debug("Importing Welcome sheet blueprint to Google.");
                    final File f = s.getOverview().latestWelcome(welcome);
                    final Spreadsheet result = copySheet(sheetsService, s.getStonky(), f, InternalSheet.WELCOME);
                    driveService.files().delete(f.getId()).execute();
                    return result;
                }));
        final BiFunction<Spreadsheet, Spreadsheet, Spreadsheet> combiner = (a, b) -> {
            if (Objects.equals(a.getSpreadsheetId(), b.getSpreadsheetId())) {
                return a;
            } else {
                throw new IllegalStateException("Should not happen.");
            }
        };
        final CompletableFuture<Spreadsheet> merged = walletCopier.thenCombine(peopleCopier, combiner)
                .thenCombine(welcomeCopier, combiner);
        LOGGER.debug("Blocking until all operations terminate.");
        final String stonkySpreadsheetId = merged.get().getSpreadsheetId();
        LOGGER.info("Stonky spreadsheet updated at: https://docs.google.com/spreadsheets/d/{}", stonkySpreadsheetId);
        return stonkySpreadsheetId;
    }

    @Override
    public Optional<String> apply(final Tenant tenant) {
        final SessionInfo sessionInfo = tenant.getSessionInfo();
        try {
            if (!credentialSupplier.credentialExists(sessionInfo)) {
                LOGGER.info("Stonky integration disabled. No Google credentials found for user '{}'.",
                            sessionInfo.getUsername());
                return Optional.empty();
            }
            return Optional.ofNullable(run(tenant));
        } catch (final Exception ex) {
            LOGGER.warn("Failed integrating with Stonky.", ex);
            return Optional.empty();
        }
    }

    private static class Summary {

        private final DriveOverview overview;
        private final Spreadsheet stonky;

        public Summary(final DriveOverview overview, final Spreadsheet stonky) {
            this.overview = overview;
            this.stonky = stonky;
        }

        public DriveOverview getOverview() {
            return overview;
        }

        public Spreadsheet getStonky() {
            return stonky;
        }
    }

    private static final class SummaryWithExport extends Summary {

        private final java.io.File export;

        public SummaryWithExport(final Summary summary, final java.io.File export) {
            super(summary.getOverview(), summary.getStonky());
            this.export = export;
        }

        public java.io.File getExport() {
            return export;
        }
    }
}
