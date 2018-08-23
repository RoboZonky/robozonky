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

import java.util.Objects;

import com.google.api.client.testing.http.MockLowLevelHttpResponse;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.SheetProperties;
import com.google.api.services.sheets.v4.model.Spreadsheet;

public class CopySheetResponseHandler extends ResponseHandler {

    private final Sheet source;
    private final Spreadsheet parent;

    public CopySheetResponseHandler(final Spreadsheet parent, final Sheet source) {
        this.parent = parent;
        this.source = source;
    }

    @Override
    protected boolean appliesTo(final String method, final String url) {
        final int id = source.getProperties().getSheetId();
        return Objects.equals(method, "POST") &&
                Objects.equals(url, "https://sheets.googleapis.com/v4/spreadsheets/" + parent.getSpreadsheetId() +
                        "/sheets/" + id + ":copyTo");
    }

    @Override
    protected MockLowLevelHttpResponse respond(final String method, final String url) {
        final SheetProperties cloned = source.getProperties().clone();
        return new MockLowLevelHttpResponse().setContent(toJson(cloned));
    }
}
