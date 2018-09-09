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
     * Google Drive's file ID of the master Stonky spreadsheet, to be copied for each user. If null, the latest version
     * of Stonky will be used.
     */
    STONKY_MASTER("robozonky.stonky.master_gdrive_id", null),
    /**
     * Google Drive's file ID to the spreadsheet where the user wants the XLS from Zonky imported. If null, will be
     * autodetected.
     */
    STONKY_COPY("robozonky.stonky.gdrive_id", null),
    /**
     * Name (not path) of the folder where RoboZonky will be looking up Google credentials. Will always be relative to
     * the current working directory.
     */
    GOOGLE_LOCAL_FOLDER("robozonky.google.local_credentials_folder", "Google"),
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
