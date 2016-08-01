/*
 * Copyright 2016 Lukáš Petrovický
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

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.ParseException;
import java.util.Properties;

import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple wrapper around a property file that replaces the unnecessarily complex commons-configuration2 which was being
 * used before.
 */
class ImmutableConfiguration {

    private static final Logger LOGGER = LoggerFactory.getLogger(ImmutableConfiguration.class);
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
     * @throws InvestmentStrategyParseException When there was a problem reading the file.
     */
    public static ImmutableConfiguration from(final File properties) throws InvestmentStrategyParseException {
        try (final Reader reader = Files.newBufferedReader(properties.toPath(), Charset.forName("UTF-8"))) {
            final Properties props = new Properties();
            props.load(reader);
            return ImmutableConfiguration.from(props);
        } catch (final IOException ex) {
            throw new InvestmentStrategyParseException("Failed reading strategy file.", ex);
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
            ImmutableConfiguration.LOGGER.trace("Property '{}' has no value.", key);
            return def;
        } else {
            ImmutableConfiguration.LOGGER.trace("Property '{}' has value '{}'.", key, result);
            return result.trim();
        }
    }

    /**
     * Whether or not the configuration contains a given key.
     * @param key Key in question.
     * @return True if contains.
     */
    public boolean containsKey(final String key) {
        return properties.containsKey(key);
    }

    /**
     * Retrieve a given configuration value and convert it to an int. Make sure to call {@link #containsKey(String)}
     * before calling this.
     *
     * @param key Key to look up.
     * @return The value for the key.
     */
    public int getInt(final String key) {
        return Integer.parseInt(this.getValue(key, "0"));
    }

    /**
     * Retrieve a given configuration value and convert it to {@link String}. Make sure to call
     * {@link #containsKey(String)} before calling this.
     *
     * @param key Key to look up.
     * @return The value for the key.
     */
    public boolean getBoolean(final String key) {
        return Boolean.parseBoolean(this.getValue(key, "false"));
    }

    /**
     * Retrieve a given configuration value and convert it to {@link BigDecimal}. Make sure to call
     * {@link #containsKey(String)} before calling this.
     *
     * @param key Key to look up.
     * @return The value for the key.
     */
    public BigDecimal getBigDecimal(final String key) {
        final String value = this.getValue(key, "0.0");
        try {
            return (BigDecimal) ImmutableConfiguration.DECIMAL_FORMAT.parse(value);
        } catch (final ParseException ex) {
            throw new IllegalStateException("Invalid value for property '" + key + "': '" + value + "'", ex);
        }
    }

}
