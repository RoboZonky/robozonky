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

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.google.api.client.http.FileContent;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

enum Export {

    WALLET("/users/me/wallet/transactions/export"),
    PEOPLE("/users/me/investments/export");

    private static final Logger LOGGER = LoggerFactory.getLogger(Export.class);
    private static final String MIME_TYPE_XLS_SPREADSHEET = "application/vnd.ms-excel";
    private static final String ZONKY_ROOT = "https://api.zonky.cz";
    private final String path;

    Export(final String path) {
        this.path = path;
    }

    private static File downloadFromUrl(final URL url) throws IOException {
        final File f = File.createTempFile("robozonky-", ".download");
        FileUtils.copyURLToFile(url, f);
        return f;
    }

    private URL formUrl(final String root, final ZonkyApiToken token) throws MalformedURLException {
        return new URL(root + path + "?access_token=" + String.valueOf(token.getAccessToken()));
    }

    public FileContent download(final ZonkyApiToken token) {
        return download(token, ZONKY_ROOT);
    }

    FileContent download(final ZonkyApiToken token, final String urlRoot) {
        try {
            LOGGER.debug("Contacting Zonky to download the export.");
            final URL url = formUrl(urlRoot, token);
            final FileContent result = new FileContent(MIME_TYPE_XLS_SPREADSHEET, downloadFromUrl(url));
            LOGGER.debug("Downloaded: {}.", result);
            return result;
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed downloading Zonky export.", ex);
        }
    }

}
