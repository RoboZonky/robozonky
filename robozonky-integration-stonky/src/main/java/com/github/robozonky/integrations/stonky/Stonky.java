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
import java.net.MalformedURLException;
import java.net.URL;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.common.jobs.Payload;
import com.github.robozonky.common.remote.ApiProvider;
import com.github.robozonky.common.remote.ZonkyApiTokenSupplier;
import com.github.robozonky.common.secrets.SecretProvider;
import com.github.robozonky.internal.api.Settings;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class Stonky implements Payload {

    private static final Logger LOGGER = LoggerFactory.getLogger(Stonky.class);
    private static final String ZONKY_ROOT = "https://api.zonky.cz/users/me";

    private static URL getUrl(final String prefix, final char... token) {
        final String attempted = prefix + String.valueOf(token);
        try {
            return new URL(attempted);
        } catch (final MalformedURLException ex) {
            throw new IllegalStateException("Failed creating URL: " + attempted, ex);
        }
    }

    private static URL getWalletXlsUrl(final ZonkyApiToken token) {
        return getUrl(ZONKY_ROOT + "/wallet/transactions/export?access_token=", token.getAccessToken());
    }

    private static URL getInvestmentsXlsUrl(final ZonkyApiToken token) {
        return getUrl(ZONKY_ROOT + "/investments/export?access_token=", token.getAccessToken());
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
                                         final String name) throws IOException {
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
                .setIndex(0)
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

    private static void run(final SessionInfo sessionInfo, final Supplier<ZonkyApiToken> zonkyApiTokenSupplier)
            throws GeneralSecurityException, IOException, ExecutionException, InterruptedException {
        LOGGER.debug("Updating Stonky spreadsheet.");
        final Drive driveService = Util.createDriveService(sessionInfo);
        final Sheets sheetsService = Util.createSheetsService(sessionInfo);
        final CompletableFuture<Summary> summary = CompletableFuture.supplyAsync(Util.wrap(() -> {
            final DriveOverview o = DriveOverview.create(sessionInfo, driveService);
            LOGGER.debug("Google Drive overview: {}.", o);
            final File s = o.latestStonky();
            final Spreadsheet result = sheetsService.spreadsheets().get(s.getId()).execute();
            return new Summary(o, result);
        }));
        final CompletableFuture<Spreadsheet> walletCopier = summary.thenApplyAsync(Util.wrap(s -> {
            LOGGER.debug("Requesting wallet export.");
            final DriveOverview o = s.getOverview();
            final File f = o.latestWallet(() -> getWalletXlsUrl(zonkyApiTokenSupplier.get()));
            return copySheet(sheetsService, s.getStonky(), f, "Wallet");
        }));
        final CompletableFuture<Spreadsheet> peopleCopier = summary.thenApplyAsync(Util.wrap(s -> {
            LOGGER.debug("Requesting investments export.");
            final DriveOverview o = s.getOverview();
            final File f = o.latestInvestments(() -> getInvestmentsXlsUrl(zonkyApiTokenSupplier.get()));
            return copySheet(sheetsService, s.getStonky(), f, "People");
        }));
        final CompletableFuture<Spreadsheet> merged = walletCopier.thenCombine(peopleCopier, (a, b) -> a);
        LOGGER.debug("Blocking until all operations terminate.");
        final String stonkySpreadsheetId = merged.get().getSpreadsheetId();
        LOGGER.info("Stonky spreadsheet updated at: https://docs.google.com/spreadsheets/d/{}", stonkySpreadsheetId);
    }

    @Override
    public void accept(final SecretProvider secretProvider) {
        final SessionInfo sessionInfo = new SessionInfo(secretProvider.getUsername());
        if (!Util.hasCredentials(sessionInfo)) {
            LOGGER.info("Stonky integration disabled. No Google credentials found for user '{}'.",
                        sessionInfo.getUsername());
            return;
        }
        try (final ApiProvider apis = new ApiProvider()) {
            run(sessionInfo, new ZonkyApiTokenSupplier(ZonkyApiToken.SCOPE_FILE_DOWNLOAD_STRING, apis, secretProvider,
                                                       Settings.INSTANCE.getTokenRefreshPeriod()));
        } catch (final Exception ex) {
            LOGGER.warn("Failed integrating with Stonky.", ex);
        }
    }

    private static final class Summary {

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
}
