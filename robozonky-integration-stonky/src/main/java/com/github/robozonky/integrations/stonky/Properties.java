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

import java.util.Optional;

import com.github.robozonky.internal.api.Settings;
import com.google.api.client.http.HttpTransport;

public enum Properties {

    /**
     * Google Drive's file ID of the master Stonky spreadsheet, to be copied for each user.
     */
    STONKY_MASTER("robozonky.stonky.master_gdrive_id", "1ZnY1hJSsIuUZVF10dtk-GI09l2_RHJTSlgH75Opi7V8"),
    /**
     * Google Drive's file ID to the spreadsheet where the user wants the XLS from Zonky imported. If blank, will be
     * autodetected.
     */
    STONKY_COPY("robozonky.stonky.gdrive_id", null),
    /**
     * To give to {@link GoogleCredentialProvider#live(HttpTransport, String, int)}.
     */
    GOOGLE_CALLBACK_HOST("robozonky.google.callback_host", "localhost"),
    /**
     * To give to {@link GoogleCredentialProvider#live(HttpTransport, String, int)}.
     */
    GOOGLE_CALLBACK_PORT("robozonky.google.callback_port", "0");

    private final String key;
    private final String defaultValue;

    Properties(final String key, final String defaultValue) {
        this.key = key;
        this.defaultValue = defaultValue;
    }

    public String getKey() {
        return key;
    }

    public Optional<String> getValue() {
        return Optional.ofNullable(Settings.INSTANCE.get(key, defaultValue));
    }
}
