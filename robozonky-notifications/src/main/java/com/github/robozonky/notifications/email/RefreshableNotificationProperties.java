/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.notifications.email;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.api.Settings;

public class RefreshableNotificationProperties extends Refreshable<NotificationProperties> {

    public static final String CONFIG_FILE_LOCATION_PROPERTY = "robozonky.notifications.email.config.file";
    static final File DEFAULT_CONFIG_FILE_LOCATION = new File("robozonky-notifications-email.cfg");

    private static String readUrl(final URL url) throws IOException {
        try (final BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream(), Defaults.CHARSET))) {
            return r.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    @Override
    protected Optional<NotificationProperties> transform(final String source) {
        try (final InputStream baos = new ByteArrayInputStream(source.getBytes(Defaults.CHARSET))) {
            final Properties p = new Properties();
            p.load(baos);
            return Optional.of(new NotificationProperties(p));
        } catch (final IOException ex) {
            LOGGER.warn("Failed transforming source.", ex);
            return Optional.empty();
        }
    }

    @Override
    protected Optional<String> getLatestSource() {
        final String propValue =
                Settings.INSTANCE.get(RefreshableNotificationProperties.CONFIG_FILE_LOCATION_PROPERTY, (String) null);
        if (propValue != null) { // attempt to read from the URL specified by the property
            LOGGER.debug("Reading notification configuration from {}.", propValue);
            try {
                return Optional.of(RefreshableNotificationProperties.readUrl(new URL(propValue)));
            } catch (final IOException ex) {
                // fall back to the property file
                LOGGER.debug("Failed reading configuration from {}.", propValue);
            }
        }
        final File defaultConfigFile = RefreshableNotificationProperties.DEFAULT_CONFIG_FILE_LOCATION;
        LOGGER.debug("Read config file {}.", defaultConfigFile.getAbsolutePath());
        try {
            final URL u = defaultConfigFile.toURI().toURL();
            final String content = RefreshableNotificationProperties.readUrl(u);
            return Optional.of(content);
        } catch (final IOException ex) {
            LOGGER.debug("Failed reading configuration file {}.", defaultConfigFile);
            return Optional.empty();
        }
    }
}
