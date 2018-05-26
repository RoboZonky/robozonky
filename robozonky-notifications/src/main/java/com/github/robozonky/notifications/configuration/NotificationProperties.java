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

package com.github.robozonky.notifications.configuration;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;

import com.github.robozonky.notifications.email.SupportedListener;
import com.github.robozonky.notifications.util.Counter;

public class NotificationProperties {

    static final String HOURLY_LIMIT = "hourlyMaxEmails";
    final protected Properties properties;
    private final Counter globalCounter;
    public NotificationProperties(final Properties source) {
        this.properties = source;
        this.globalCounter = new Counter("global", this.getGlobalHourlyLimit(), Duration.ofHours(1));
    }

    static String getCompositePropertyName(final SupportedListener listener, final String property) {
        return listener.getLabel() + "." + property;
    }

    protected Properties getProperties() {
        return this.properties;
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

    public boolean isListenerEnabled(final SupportedListener listener) {
        if (listener == SupportedListener.TESTING) {
            return true;
        } else {
            final String propName = NotificationProperties.getCompositePropertyName(listener, "enabled");
            return this.isEnabled() && this.getBooleanValue(propName, false);
        }
    }

    protected int getGlobalHourlyLimit() {
        final int val = this.getIntValue(NotificationProperties.HOURLY_LIMIT, Integer.MAX_VALUE);
        return (val < 0) ? Integer.MAX_VALUE : val;
    }

    public Counter getGlobalCounter() {
        return globalCounter;
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
        return properties.hashCode();
    }
}
