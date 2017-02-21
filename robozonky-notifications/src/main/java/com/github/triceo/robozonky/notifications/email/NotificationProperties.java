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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.internal.api.Defaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class NotificationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProperties.class);
    protected static final String HOURLY_LIMIT = "hourlyMaxEmails";
    static final String CONFIG_FILE_LOCATION_PROPERTY = "robozonky.notifications.email.config.file";
    static final File DEFAULT_CONFIG_FILE_LOCATION = new File("robozonky-notifications.cfg");

    static Optional<NotificationProperties> getProperties(final String source) {
        try (final ByteArrayInputStream baos = new ByteArrayInputStream(source.getBytes(Defaults.CHARSET))) {
            final Properties p = new Properties();
            p.load(baos);
            return Optional.of(new NotificationProperties(p));
        } catch (final IOException ex) {
            NotificationProperties.LOGGER.warn("Failed transforming source.", ex);
            return Optional.empty();
        }
    }

    private static String readUrl(final URL url) throws IOException {
        try (final BufferedReader r = new BufferedReader(new InputStreamReader(url.openStream(), Defaults.CHARSET))) {
            return r.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }

    static Optional<String> getPropertiesContents() {
        final String propValue = System.getProperty(NotificationProperties.CONFIG_FILE_LOCATION_PROPERTY);
        if (propValue != null) { // attempt to read from the URL specified by the property
            NotificationProperties.LOGGER.debug("Reading e-mail notification configuration from {}.", propValue);
            try {
                return Optional.of(NotificationProperties.readUrl(new URL(propValue)));
            } catch (final IOException ex) {
                // fall back to the property file
                NotificationProperties.LOGGER.debug("Failed reading configuration from {}.", propValue);
            }
        }
        if (NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.canRead()) {
            try {
                NotificationProperties.LOGGER.debug("Read config file {}.",
                        NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.getAbsolutePath());
                final URL u = NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION.toURI().toURL();
                final String content = NotificationProperties.readUrl(u);
                return Optional.of(content);
            } catch (final IOException ex) {
                NotificationProperties.LOGGER.debug("Failed reading configuration file {}.",
                        NotificationProperties.DEFAULT_CONFIG_FILE_LOCATION, ex);
                return Optional.empty();
            }
        } else {
            NotificationProperties.LOGGER.debug("No configuration file found.");
            return Optional.empty();
        }
    }

    protected static String getCompositePropertyName(final SupportedListener listener, final String property) {
        return listener.getId() + "." + property;
    }

    protected Properties properties;
    private String localHostAddress;
    private final EmailCounter globalEmailCounter;

    NotificationProperties(final Properties source) {
        this.properties = source;
        this.globalEmailCounter = new EmailCounter(this.getGlobalHourlyEmailLimit());
    }

    public synchronized String getLocalHostAddress() {
        if (localHostAddress == null) {
            // lazy init so that the remote request penalty is not incurred needlessly
            localHostAddress = Defaults.getHostAddress();
        }
        return localHostAddress;
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

    public String getSender() {
        return this.getStringValue("from", "noreply@robozonky.cz");
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

    private int getGlobalHourlyEmailLimit() {
        final int val = this.getIntValue(NotificationProperties.HOURLY_LIMIT)
                .orElse(Integer.MAX_VALUE);
        if (val < 0) {
            return Integer.MAX_VALUE;
        } else {
            return val;
        }
    }

    public EmailCounter getGlobalEmailCounter() {
        return globalEmailCounter;
    }

    public boolean isListenerEnabled(final SupportedListener listener) {
        return this.getBooleanValue(NotificationProperties.getCompositePropertyName(listener, "enabled"), false);
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final NotificationProperties that = (NotificationProperties) o;
        return Objects.equals(properties, that.properties);
    }

    @Override
    public int hashCode() {
        return Objects.hash(properties);
    }

    @Override
    public String toString() {
        try (final StringWriter sw = new StringWriter()) {
            properties.store(sw, "");
            return sw.toString();
        } catch (final IOException ex) {
            NotificationProperties.LOGGER.warn("Failed converting properties to string.", ex);
            return "";
        }
    }
}
