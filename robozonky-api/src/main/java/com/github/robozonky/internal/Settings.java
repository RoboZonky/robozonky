/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal;

import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * These are RoboZonky settings read from a property file at system startup. The location of this file will be looked
 * up in a property {@link #FILE_LOCATION_PROPERTY}. Defaults for all settings looked up through this class come from
 * {@link System#getProperties()}.
 */
public enum Settings {

    INSTANCE; // cheap thread-safe singleton

    public static final String FILE_LOCATION_PROPERTY = "robozonky.properties.file";
    private static final int HTTPS_DEFAULT_PORT = 443;
    private static final Logger LOGGER = LogManager.getLogger(Settings.class);
    private final AtomicReference<Properties> properties = new AtomicReference<>();

    private Properties getProperties() {
        var propertyFilename = System.getProperty(Settings.FILE_LOCATION_PROPERTY);
        if (propertyFilename == null) {
            return new Properties();
        }
        var propertyFile = new File(propertyFilename);
        if (!propertyFile.exists()) {
            throw new IllegalStateException("Properties file does not exist: " + propertyFile.getAbsolutePath());
        }
        try (var r = Files.newBufferedReader(propertyFile.toPath(), Defaults.CHARSET)) {
            var props = new Properties();
            props.load(r);
            LOGGER.debug("Loaded from '{}'.", propertyFile.getAbsolutePath());
            return props;
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot read properties.", ex);
        }
    }

    /**
     * To be used purely for testing purposes.
     */
    void reinit() {
        this.properties.set(null);
    }

    public <T> T get(final String key, final Function<String, T> adapter) {
        final Properties props = this.properties.updateAndGet(p -> { // lazy initialization to allow for reinit
            if (p == null) {
                return this.getProperties();
            } else {
                return p;
            }
        });
        final String val = props.containsKey(key) ? props.getProperty(key) : System.getProperty(key);
        return adapter.apply(val);
    }

    public String get(final String key, final String defaultValue) {
        return get(key, value -> value == null ? defaultValue : value);
    }

    public int get(final String key, final int defaultValue) {
        return get(key, value -> {
            try {
                return Integer.parseInt(value);
            } catch (final NumberFormatException ex) {
                return defaultValue;
            }
        });
    }

    public <T> T get(final String key, final Function<String, T> adapter, final T defaultValue) {
        return get(key, value -> {
            if (value == null) {
                return defaultValue;
            } else {
                return adapter.apply(value);
            }
        });
    }

    public boolean get(final String key) {
        return get(key, Boolean::parseBoolean);
    }

    public <T> T get(final Settings.Key key, final Function<String, T> adapter, final T defaultValue) {
        return get(key.getName(), adapter, defaultValue);
    }

    public String get(final Settings.Key key, final String defaultValue) {
        return get(key.getName(), defaultValue);
    }

    public int get(final Settings.Key key, final int defaultValue) {
        return get(key.getName(), defaultValue);
    }

    public boolean get(final Settings.Key key) {
        return get(key.getName());
    }

    public boolean isDebugHttpResponseLoggingEnabled() {
        return get(Settings.Key.DEBUG_ENABLE_HTTP_RESPONSE_LOGGING);
    }

    public Duration getRemoteResourceRefreshInterval() {
        return Duration.ofMinutes(get(Settings.Key.DEFAULTS_RESOURCE_REFRESH, 5));
    }

    public Duration getSocketTimeout() {
        return Duration.ofSeconds(get(Settings.Key.DEFAULTS_SOCKET_TIMEOUT, 10));
    }

    public Duration getConnectionTimeout() {
        return Duration.ofSeconds(get(Settings.Key.DEFAULTS_CONNECTION_TIMEOUT, 10));
    }

    public Optional<String> getHttpsProxyHostname() {
        return Optional.ofNullable(get(Key.HTTPS_PROXY_HOSTNAME, null));
    }

    public int getHttpsProxyPort() {
        return get(Key.HTTPS_PROXY_PORT, s -> {
            try {
                return Integer.parseInt(s);
            } catch (final Exception ex) {
                return HTTPS_DEFAULT_PORT;
            }
        }, HTTPS_DEFAULT_PORT);
    }

    public int getDryRunBalanceMinimum() {
        return get(Settings.Key.DRY_RUN_BALANCE_MINIMUM, -1);
    }

    public int getMaxItemsReadFromPrimaryMarketplace() {
        return get(Key.MAX_ITEMS_READ_FROM_PRIMARY_MARKETPLACE, -1);
    }

    public int getMaxItemsReadFromSecondaryMarketplace() {
        return get(Key.MAX_ITEMS_READ_FROM_SECONDARY_MARKETPLACE, 1000);
    }

    public int getDefaultApiPageSize() {
        return get(Settings.Key.DEFAULTS_API_PAGE_SIZE, 100);
    }

    public enum Key {

        DEBUG_ENABLE_HTTP_RESPONSE_LOGGING("robozonky.debug.enable_http_response_logging"),
        DEFAULTS_RESOURCE_REFRESH("robozonky.default.resource_refresh_minutes"),
        DEFAULTS_SOCKET_TIMEOUT("robozonky.default.socket_timeout_seconds"),
        DEFAULTS_CONNECTION_TIMEOUT("robozonky.default.connection_timeout_seconds"),
        DEFAULTS_API_PAGE_SIZE("robozonky.default.api_page_size"),
        DRY_RUN_BALANCE_MINIMUM("robozonky.dry_run_balance_minimum"),
        MAX_ITEMS_READ_FROM_PRIMARY_MARKETPLACE("robozonky.max_items_read_from_primary_marketplace"),
        MAX_ITEMS_READ_FROM_SECONDARY_MARKETPLACE("robozonky.max_items_read_from_secondary_marketplace"),
        STATE_FILE_LOCATION("robozonky.state_file"),
        HTTPS_PROXY_HOSTNAME("https.proxyHost"),
        HTTPS_PROXY_PORT("https.proxyPort");

        private final String name;

        Key(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

}
