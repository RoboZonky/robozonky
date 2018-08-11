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
import java.util.function.Supplier;

import com.github.robozonky.api.SessionInfo;
import com.github.robozonky.api.jobs.Payload;
import com.github.robozonky.api.remote.entities.ZonkyApiToken;
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

    private static URL getUrl(final String prefix, final char... token) {
        try {
            return new URL(prefix + String.valueOf(token));
        } catch (final MalformedURLException ex) {
            throw new IllegalStateException("Failed downloading file.", ex);
        }
    }

    private static URL getWalletXlsUrl(final ZonkyApiToken token) {
        return getUrl("https://api.zonky.cz/users/me/wallet/transactions/export?access_token=", token.getAccessToken());
    }

    private static URL getInvestmentsXlsUrl(final ZonkyApiToken token) {
        return getUrl("https://api.zonky.cz/users/me/investments/export?access_token=", token.getAccessToken());
    }

    private SheetProperties copySheet(final Sheets sheetsService, final Spreadsheet stonky, final File sheet) throws
            IOException {
        final int sheetId = sheetsService.spreadsheets().get(sheet.getId())
                .execute()
                .getSheets()
                .get(0) // first and only sheet
                .getProperties()
                .getSheetId();
        final CopySheetToAnotherSpreadsheetRequest r = new CopySheetToAnotherSpreadsheetRequest()
                .setDestinationSpreadsheetId(stonky.getSpreadsheetId());
        return sheetsService.spreadsheets().sheets()
                .copyTo(sheet.getId(), sheetId, r)
                .execute()
                .clone();
    }

    private void copySheet(final Sheets sheetsService, final Spreadsheet stonky, final File sheet,
                           final String name) throws
            IOException {
        LOGGER.debug("Processing sheet '{}'.", name);
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
        final SheetProperties newSheet = copySheet(sheetsService, stonky, sheet)
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
        LOGGER.debug("Stonky `{}` sheet processed.", name);
    }

    private void acceptFailing(final SessionInfo sessionInfo,
                               final Supplier<ZonkyApiToken> zonkyApiTokenSupplier) throws GeneralSecurityException,
            IOException {
        // see what we have in the drive, to see what we need to create or update
        final Drive driveService = Util.createDriveService(sessionInfo);
        final DriveOverview overview = DriveOverview.create(sessionInfo, driveService);
        // clone Stonky if necessary
        final File stonky = overview.latestStonky();
        // upload to Google Drive
        final File wallet =
                overview.offerLatestWalletSpreadsheet(() -> getWalletXlsUrl(zonkyApiTokenSupplier.get()));
        final File investments =
                overview.offerInvestmentsSpreadsheet(() -> getInvestmentsXlsUrl(zonkyApiTokenSupplier.get()));
        final Sheets sheetsService = Util.createSheetsService(sessionInfo);
        final Spreadsheet stonkySpreadsheet = sheetsService.spreadsheets().get(stonky.getId()).execute();
        copySheet(sheetsService, stonkySpreadsheet, wallet, "Wallet");
        copySheet(sheetsService, stonkySpreadsheet, investments, "People");
        LOGGER.info("Stonky spreadsheet updated at: https://docs.google.com/spreadsheets/d/{}",
                    stonkySpreadsheet.getSpreadsheetId());
    }

    @Override
    public void accept(final SessionInfo sessionInfo, final Supplier<ZonkyApiToken> zonkyApiTokenSupplier) {
        try {
            acceptFailing(sessionInfo, zonkyApiTokenSupplier);
        } catch (final Exception ex) {
            throw new IllegalStateException("Failed integrating with Stonky.", ex);
        }
    }
}
