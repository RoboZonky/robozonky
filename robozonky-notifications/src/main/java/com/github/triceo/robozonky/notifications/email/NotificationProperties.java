/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.notifications.email;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NotificationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProperties.class);
    static final String CONFIG_FILE_LOCATION_PROPERTY = "robozonky.notifications.email.config.file";
    static final File DEFAULT_CONFIG_FILE_LOCATION = new File("robozonky-notifications.cfg");

    private static Properties loadProperties(final URL url) throws IOException {
        final Properties properties = new Properties();
        try (final InputStream i = url.openStream()) {
            properties.load(i);
        }
        return properties;
    }

    static Optional<NotificationProperties> getProperties() {
        final String propValue = System.getProperty(NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY);
        if (propValue != null) { // attempt to read from the URL specified by the property
            NotificationProperties.LOGGER.debug("Reading e-mail notification configuration from '{}'.", propValue);
            try {
                final Properties p = NotificationProperties.loadProperties(new URL(propValue));
                return Optional.of(new NotificationProperties(p));
            } catch (final IOException ex) {
                // fall back to the property file
                NotificationProperties.LOGGER.debug("Failed reading '{}'.", propValue);
            }
        }
        if (!NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.canRead()) {
            NotificationProperties.LOGGER.debug("Failed reading configuration file '{}'.",
                    NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.getAbsolutePath());
            return Optional.empty();
        }
        try {
            final URL url = NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.toURI().toURL();
            final Properties props = NotificationProperties.loadProperties(url);
            NotificationProperties.LOGGER.debug("Read '{}'.", url);
            return Optional.of(new NotificationProperties(props));
        } catch (final IOException ex) {
            NotificationProperties.LOGGER.debug("Failed reading '{}'.",
                    NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.getAbsolutePath(), ex);
            return Optional.empty();
        }
    }

    protected static String getCompositePropertyName(final SupportedListener listener, final String property) {
        return listener.getId() + "." + property;
    }

    protected Properties properties;

    NotificationProperties(final Properties source) {
        this.properties = source;
    }

    protected boolean getBooleanValue(final String propertyName, final boolean defaultValue) {
        final String result = this.properties.getProperty(propertyName, String.valueOf(defaultValue));
        return Boolean.valueOf(result);
    }

    protected Optional<String> getStringValue(final String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            return Optional.of(this.properties.getProperty(propertyName));
        } else {
            return Optional.empty();
        }
    }

    protected String getStringValue(final String propertyName, final String defaultValue) {
        return this.getStringValue(propertyName).orElse(defaultValue);
    }

    protected OptionalInt getIntValue(final String propertyName) {
        if (this.properties.containsKey(propertyName)) {
            return OptionalInt.of(Integer.parseInt(this.properties.getProperty(propertyName)));
        } else {
            return OptionalInt.empty();
        }
    }

    protected int getIntValue(final String propertyName, final int defaultValue) {
        return this.getIntValue(propertyName).orElse(defaultValue);
    }

    public boolean isEnabled() {
        return this.getBooleanValue("enabled", false);
    }

    public String getRecipient() {
        return this.getStringValue("to", "");
    }

    public boolean isStartTlsRequired() {
        return this.getBooleanValue("smtp.requiresStartTLS", false);
    }

    public boolean isSslOnConnectRequired() {
        return this.getBooleanValue("smtp.requiresSslOnConnect", false);
    }

    public String getSmtpUsername() {
        return this.getStringValue("smtp.username", this.getRecipient());
    }

    public String getSmtpPassword() {
        return this.getStringValue("smtp.password", "");
    }

    public String getSmtpHostname() {
        return this.getStringValue("smtp.hostname", "localhost");
    }

    public int getSmtpPort() {
        return this.getIntValue("smtp.port", 25);
    }

    public boolean isListenerEnabled(final SupportedListener listener) {
        return this.getBooleanValue(NotificationProperties.getCompositePropertyName(listener, "enabled"), false);
    }

}
