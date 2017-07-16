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

package com.github.triceo.robozonky.strategy.simple;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Optional;
import java.util.Properties;

import com.github.triceo.robozonky.internal.api.Defaults;

/**
 * Simple wrapper around a property file.
 */
class ImmutableConfiguration {

    private static final DecimalFormat DECIMAL_FORMAT;

    static {
        final DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator(',');
        symbols.setDecimalSeparator('.');
        final String pattern = "#,##0.0#";
        DECIMAL_FORMAT = new DecimalFormat(pattern, symbols);
        ImmutableConfiguration.DECIMAL_FORMAT.setParseBigDecimal(true);
    }

    /**
     * Load the strategy from a file, representing a {@link Properties} file.
     * @param properties The file in question.
     * @return The strategy available for reading.
     */
    public static ImmutableConfiguration from(final InputStream properties) {
        try (final Reader reader = new BufferedReader(new InputStreamReader(properties, Defaults.CHARSET))) {
            final Properties props = new Properties();
            props.load(reader);
            return ImmutableConfiguration.from(props);
        } catch (final IOException ex) {
            throw new IllegalStateException("Failed reading strategy.", ex);
        }
    }

    /**
     * Load the strategy directly from properties.
     * @param properties Properties in question.
     * @return The strategy available for reading.
     */
    public static ImmutableConfiguration from(final Properties properties) {
        return new ImmutableConfiguration(properties);
    }

    private final Properties properties;

    private ImmutableConfiguration(final Properties properties) {
        this.properties = properties;
    }

    private String getValue(final String key, final String def) {
        final String result = properties.getProperty(key);
        if (result == null) {
            return def;
        } else {
            return result.trim();
        }
    }

    /**
     * Whether or not the configuration contains a given key.
     * @param key Key in question.
     * @return True if contains.
     */
    boolean containsKey(final String key) {
        return properties.containsKey(key);
    }

    /**
     * Retrieve a given configuration value and convert it to an int.
     *
     * @param key Key to look up.
     * @return The value for the key.
     */
    public Optional<Integer> getInt(final String key) {
        if (this.containsKey(key)) {
            return Optional.of(Integer.parseInt(this.getValue(key, "0")));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Retrieve a given configuration value and convert it to {@link String}.
     *
     * @param key Key to look up.
     * @return The value for the key.
     */
    public Optional<Boolean> getBoolean(final String key) {
        if (this.containsKey(key)) {
            return Optional.of(Boolean.parseBoolean(this.getValue(key, "false")));
        } else {
            return Optional.empty();
        }
    }

    /**
     * Retrieve a given configuration value and convert it to {@link BigDecimal}.
     *
     * @param key Key to look up.
     * @return The value for the key.
     */
    public Optional<BigDecimal> getBigDecimal(final String key) {
        if (!this.containsKey(key)) {
            return Optional.empty();
        }
        final String value = this.getValue(key, "0.0");
        try {
            return Optional.of((BigDecimal) ImmutableConfiguration.DECIMAL_FORMAT.parse(value));
        } catch (final ParseException ex) {
            throw new IllegalStateException("Invalid value for property '" + key + "': '" + value + "'", ex);
        }
    }

}
