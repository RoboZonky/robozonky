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

package com.github.triceo.robozonky.internal.api;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.time.Duration;
import java.time.temporal.TemporalAmount;
import java.util.Properties;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.enums.Rating;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * These are RoboZonky settings read from a property file at system startup. The location of this file will be looked
 * up in a property {@link #FILE_LOCATION_PROPERTY}. Defaults for all settings looked up through this class come from
 * {@link System#getProperties()}.
 */
public enum Settings {

    INSTANCE; // cheap thread-safe singleton

    public enum Key {

        DEBUG_ENABLE_EVENT_STORAGE("robozonky.debug.enable_event_storage"),
        DEBUG_ENABLE_HTTP_RESPONSE_LOGGING("robozonky.debug.enable_http_response_logging"),
        DEFAULTS_TOKEN_REFRESH("robozonky.default.token_refresh_seconds"),
        DEFAULTS_RESOURCE_REFRESH("robozonky.default.resource_refresh_minutes"),
        DEFAULTS_SOCKET_TIMEOUT("robozonky.default.socket_timeout_seconds"),
        DEFAULTS_CONNECTION_TIMEOUT("robozonky.default.connection_timeout_seconds"),
        DEFAULTS_CAPTCHA_DELAY("robozonky.default.captcha_protection_seconds"),
        DEFAULTS_DRY_RUN_BALANCE("robozonky.default.dry_run_balance"),
        DEFAULTS_API_PAGE_SIZE("robozonky.default.api_page_size"),
        CAPTCHA_DELAY_AAAAA("robozonky.aaaaa_loan_protection_seconds"),
        CAPTCHA_DELAY_AAAA("robozonky.aaaa_loan_protection_seconds"),
        CAPTCHA_DELAY_AAA("robozonky.aaa_loan_protection_seconds"),
        CAPTCHA_DELAY_AA("robozonky.aa_loan_protection_seconds"),
        CAPTCHA_DELAY_A("robozonky.a_loan_protection_seconds"),
        CAPTCHA_DELAY_B("robozonky.b_loan_protection_seconds"),
        CAPTCHA_DELAY_C("robozonky.c_loan_protection_seconds"),
        CAPTCHA_DELAY_D("robozonky.d_loan_protection_seconds");

        private final String name;

        Key(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

    }

    public static final String FILE_LOCATION_PROPERTY = "robozonky.properties.file";
    private final Logger LOGGER = LoggerFactory.getLogger(Settings.class);

    private Properties getProperties() {
        final String filename = System.getProperty(Settings.FILE_LOCATION_PROPERTY);
        if (filename == null) {
            return new Properties();
        }
        final File f = new File(filename);
        if (!f.exists()) {
            throw new IllegalStateException("Properties file does not exist: " + f.getAbsolutePath());
        }
        try (final Reader r = Files.newBufferedReader(f.toPath(), Defaults.CHARSET)) {
            final Properties p = new Properties();
            p.load(r);
            LOGGER.debug("Loaded from '{}'.", f.getAbsolutePath());
            return p;
        } catch (final IOException ex) {
            throw new IllegalStateException("Cannot read properties.", ex);
        }
    }

    private final Properties properties = this.getProperties();

    public <T> T get(final String key, final Function<String, T> adapter) {
        final String val = properties.containsKey(key) ? properties.getProperty(key) : System.getProperty(key);
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

    public int get(final Settings.Key key, final int defaultValue) {
        return get(key.getName(), defaultValue);
    }

    /**
     * When set to true, this is essentially a controlled memory leak. Generally only useful for testing.
     * @return
     */
    public boolean isDebugEventStorageEnabled() {
        return get("robozonky.debug.enable_event_storage");
    }

    public int getTokenRefreshBeforeExpirationInSeconds() {
        return get("robozonky.default.token_refresh_seconds", 60);
    }

    public int getRemoteResourceRefreshIntervalInMinutes() {
        return get("robozonky.default.resource_refresh_minutes", 5);
    }

    public TemporalAmount getSocketTimeout() {
        return Duration.ofSeconds(get("robozonky.default.socket_timeout_seconds", 5));
    }

    public TemporalAmount getConnectionTimeout() {
        return Duration.ofSeconds(get("robozonky.default.connection_timeout_seconds", 5));
    }

    private Settings.Key getRatingKey(final Rating r) {
        switch (r) {
            case AAAAA:
                return Settings.Key.CAPTCHA_DELAY_AAAAA;
            case AAAA:
                return Settings.Key.CAPTCHA_DELAY_AAAA;
            case AAA:
                return Settings.Key.CAPTCHA_DELAY_AAA;
            case AA:
                return Settings.Key.CAPTCHA_DELAY_AA;
            case A:
                return Settings.Key.CAPTCHA_DELAY_A;
            case B:
                return Settings.Key.CAPTCHA_DELAY_B;
            case C:
                return Settings.Key.CAPTCHA_DELAY_C;
            case D:
                return Settings.Key.CAPTCHA_DELAY_D;
            default:
                throw new IllegalStateException("Impossible");
        }
    }

    public TemporalAmount getCaptchaDelay(final Rating r) {
        return get(getRatingKey(r), (delay) -> Duration.ofSeconds(Integer.parseInt(delay)), r.getCaptchaDelay());
    }

    public TemporalAmount getCaptchaDelay() {
        return Duration.ofSeconds(get(Settings.Key.DEFAULTS_CAPTCHA_DELAY, 120));
    }

    public int getDefaultDryRunBalance() {
        return get("robozonky.default.dry_run_balance", -1);
    }


}
