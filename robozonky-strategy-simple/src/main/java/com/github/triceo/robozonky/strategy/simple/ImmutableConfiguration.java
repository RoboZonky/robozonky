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
import java.util.Properties;

import com.github.triceo.robozonky.strategy.InvestmentStrategyParseException;

/**
 * Simple wrapper around a property file that replaces the unnecessarily complex commons-configuration2 which was being
 * used before.
 */
class ImmutableConfiguration {

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
        return Integer.parseInt(properties.getProperty(key));
    }

    /**
     * Retrieve a given configuration value and convert it to {@link String}. Make sure to call
     * {@link #containsKey(String)} before calling this.
     *
     * @param key Key to look up.
     * @return The value for the key.
     */
    public boolean getBoolean(final String key) {
        return Boolean.parseBoolean(properties.getProperty(key));
    }

    /**
     * Retrieve a given configuration value and convert it to {@link BigDecimal}. Make sure to call
     * {@link #containsKey(String)} before calling this.
     *
     * @param key Key to look up.
     * @return The value for the key.
     */
    public BigDecimal getBigDecimal(final String key) {
        return new BigDecimal(properties.getProperty(key));
    }

}
