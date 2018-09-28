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
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;

import com.google.api.client.util.DateTime;
import com.google.api.services.drive.model.File;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;

final class GoogleUtil {

    public static File getFolder(final String name) {
        final File result = getFile(name);
        result.setMimeType(DriveOverview.MIME_TYPE_FOLDER);
        return result;
    }

    public static java.io.File getDownloaded() {
        try {
            return java.io.File.createTempFile("robozonky-", ".testing");
        } catch (final IOException ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static File getFile(final String name) {
        return getFile(name, UUID.randomUUID().toString());
    }

    public static File getFile(final String name, final String id) {
        final File result = new File();
        result.setId(id);
        result.setMimeType("application/vnd.google-apps.files");
        result.setName(name);
        result.setModifiedTime(new DateTime(System.currentTimeMillis()));
        return result;
    }

    public static File getSpreadsheetFile(final String name) {
        return getSpreadsheetFile(name, UUID.randomUUID().toString());
    }

    public static File getSpreadsheetFile(final String name, final String id) {
        final File result = getFile(name, id);
        result.setMimeType(DriveOverview.MIME_TYPE_GOOGLE_SPREADSHEET);
        return result;
    }

    private static final Random RANDOM = new Random();

    private static Sheet getSheet(final String name, final int index) {
        final SheetProperties p1 = new SheetProperties();
        p1.setTitle(name);
        p1.setIndex(index);
        p1.setSheetId(RANDOM.nextInt(100_000));
        final Sheet sheet = new Sheet(); // create both sheets in all spreadsheets, to make testing easier
        sheet.setProperties(p1);
        return sheet;
    }

    public static Spreadsheet toSpreadsheet(final File file) {
        final Spreadsheet result = new Spreadsheet();
        result.setSpreadsheetId(file.getId());
        // add both Wallet and People sheets to all spreadsheets, just to make testing easier
        result.setSheets(Arrays.asList(getSheet("Wallet", 0), getSheet("People", 1)));
        return result;
    }
}
