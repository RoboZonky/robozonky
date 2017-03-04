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

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carries default values for some basic application properties, such as charset or locale.
 */
public final class Defaults {

    private static final Logger LOGGER = LoggerFactory.getLogger(Defaults.class);

    public static final Locale LOCALE = Locale.forLanguageTag("cs_CZ");
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Prague");
    public static final int MINIMUM_INVESTMENT_IN_CZK = 200;
    public static final int MINIMUM_INVESTMENT_INCREMENT_IN_CZK = 200;
    public static final String ROBOZONKY_VERSION = Defaults.class.getPackage().getImplementationVersion();
    public static final String ROBOZONKY_URL = "http://www.robozonky.cz";
    public static final String ROBOZONKY_USER_AGENT =
            "RoboZonky/" + Defaults.ROBOZONKY_VERSION + " (" + Defaults.ROBOZONKY_URL + ")";

    /**
     * Will execute call to a remote web service which will return the external IP address of this machine.
     *
     * @return Whatever the web service returned as the address, or "localhost" if the remote call failed.
     */
    public static String getHostAddress() {
        final String url = "http://checkip.amazonaws.com";
        try (final BufferedReader in =
                     new BufferedReader(new InputStreamReader(new URL(url).openStream(), Defaults.CHARSET))) {
            return in.readLine();
        } catch (final Exception ex) {
            Defaults.LOGGER.debug("Failed retrieving local host address.", ex);
            return "localhost";
        }
    }

    private static int getPropertyValue(final String propertyName, final int defaultValue) {
        final String value = System.getProperty(propertyName, String.valueOf(defaultValue));
        try {
            return Integer.parseInt(value);
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    private static boolean getPropertyValue(final String propertyName, final boolean defaultValue) {
        final String value = System.getProperty(propertyName, String.valueOf(defaultValue));
        try {
            return Boolean.parseBoolean(value);
        } catch (final NumberFormatException ex) {
            return defaultValue;
        }
    }

    /**
     * When set to true, this is essentially a controlled memory leak. Generally only useful for testing.
     * @return
     */
    public static boolean isDebugEventStorageEnabled() {
        return Defaults.getPropertyValue("robozonky.debug.enable_event_storage", false);
    }

    public static int getTokenRefreshBeforeExpirationInSeconds() {
        return Defaults.getPropertyValue("robozonky.default.token_refresh_seconds", 60);
    }

    public static int getRemoteResourceRefreshIntervalInMinutes() {
        return Defaults.getPropertyValue("robozonky.default.resource_refresh_minutes", 5);
    }

    public static int getCaptchaDelayInSeconds() {
        return Defaults.getPropertyValue("robozonky.default.captcha_protection_seconds", 120);
    }

    public static int getDefaultDryRunBalance() {
        return Defaults.getPropertyValue("robozonky.default.dry_run_balance", -1);
    }

}
