/*
 * Copyright 2019 The RoboZonky Project
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

package com.github.robozonky.internal.api;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import com.github.robozonky.api.remote.enums.Rating;
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
    private final Logger LOGGER = LogManager.getLogger(Settings.class);
    private final AtomicReference<Properties> properties = new AtomicReference<>();

    private static Settings.Key getRatingKey(final Rating r) {
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

    public <T> T get(final Settings.Key key, final Function<String, T> adapter) {
        return get(key.getName(), adapter);
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
        return Duration.ofSeconds(get(Settings.Key.DEFAULTS_SOCKET_TIMEOUT, 60));
    }

    public Duration getConnectionTimeout() {
        return Duration.ofSeconds(get(Settings.Key.DEFAULTS_CONNECTION_TIMEOUT, 60));
    }

    public Duration getCaptchaDelay(final Rating r) {
        return get(getRatingKey(r), delay -> Duration.ofSeconds(Long.parseLong(delay)), getCaptchaDelay());
    }

    public Optional<String> getHttpsProxyHostname() {
        return Optional.ofNullable(get(Key.HTTPS_PROXY_HOSTNAME, (String) null));
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

    /**
     * The default delay for CAPTCHA protected loans. Right now, it is 0 due to the fact that no loans are protected by
     * CAPTCHA. If this ever changes, the delay MUST be increased here, as other bits of code - such as marketplace
     * activity detection - depend on it.
     * @return Duration of CAPTCHA protection for a standard loan.
     */
    public Duration getCaptchaDelay() {
        return Duration.ofSeconds(get(Settings.Key.DEFAULTS_CAPTCHA_DELAY, 0));
    }

    public int getDryRunBalanceMinimum() {
        return get(Settings.Key.DRY_RUN_BALANCE_MINIMUM, -1);
    }

    public int getDefaultApiPageSize() {
        return get(Settings.Key.DEFAULTS_API_PAGE_SIZE, 50);
    }

    public enum Key {

        DEBUG_ENABLE_HTTP_RESPONSE_LOGGING("robozonky.debug.enable_http_response_logging"),
        DEFAULTS_RESOURCE_REFRESH("robozonky.default.resource_refresh_minutes"),
        DEFAULTS_SOCKET_TIMEOUT("robozonky.default.socket_timeout_seconds"),
        DEFAULTS_CONNECTION_TIMEOUT("robozonky.default.connection_timeout_seconds"),
        DEFAULTS_CAPTCHA_DELAY("robozonky.default.captcha_protection_seconds"),
        DEFAULTS_API_PAGE_SIZE("robozonky.default.api_page_size"),
        CAPTCHA_DELAY_AAAAA("robozonky.aaaaa_loan_protection_seconds"),
        CAPTCHA_DELAY_AAAA("robozonky.aaaa_loan_protection_seconds"),
        CAPTCHA_DELAY_AAA("robozonky.aaa_loan_protection_seconds"),
        CAPTCHA_DELAY_AA("robozonky.aa_loan_protection_seconds"),
        CAPTCHA_DELAY_A("robozonky.a_loan_protection_seconds"),
        CAPTCHA_DELAY_B("robozonky.b_loan_protection_seconds"),
        CAPTCHA_DELAY_C("robozonky.c_loan_protection_seconds"),
        CAPTCHA_DELAY_D("robozonky.d_loan_protection_seconds"),
        DRY_RUN_BALANCE_MINIMUM("robozonky.dry_run_balance_minimum"),
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
