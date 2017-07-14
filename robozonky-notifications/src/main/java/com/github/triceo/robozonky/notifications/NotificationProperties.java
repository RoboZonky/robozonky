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

package com.github.triceo.robozonky.notifications;

import java.io.IOException;
import java.io.StringWriter;
import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class NotificationProperties {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationProperties.class);

    protected Properties getProperties() {
        return this.properties;
    }

    final protected Properties properties;
    private final Counter globalCounter;

    protected NotificationProperties(final Properties source) {
        this.properties = source;
        this.globalCounter = new Counter("global", this.getGlobalHourlyLimit(), Duration.ofHours(1));
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

    protected abstract int getGlobalHourlyLimit();

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
