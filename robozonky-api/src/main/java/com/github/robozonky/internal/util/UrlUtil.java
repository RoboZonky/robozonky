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

package com.github.robozonky.internal.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import com.github.robozonky.internal.Settings;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public final class UrlUtil {

    private static final Logger LOGGER = LogManager.getLogger(UrlUtil.class);

    private UrlUtil() {
        // no instances
    }

    public static URL toURL(final String string) {
        try {
            LOGGER.trace("Converting '{}' to URL.", string);
            return new URL(string);
        } catch (final MalformedURLException ex) {
            final File f = new File(string);
            try {
                LOGGER.trace("Converting '{}' to URL as a file.", string);
                return f.getAbsoluteFile().toURI().toURL();
            } catch (final MalformedURLException ex2) {
                throw new IllegalArgumentException("Incorrect location configuration.", ex2);
            }
        }
    }

    public static InputStream open(final URL url) throws IOException {
        LOGGER.trace("Opening {}.", url);
        final URLConnection con = url.openConnection();
        con.setConnectTimeout((int) Settings.INSTANCE.getConnectionTimeout().toMillis());
        con.setReadTimeout((int) Settings.INSTANCE.getSocketTimeout().toMillis());
        return con.getInputStream();
    }
}
