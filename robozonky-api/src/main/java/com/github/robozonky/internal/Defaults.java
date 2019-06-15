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

package com.github.robozonky.internal;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Currency;
import java.util.Locale;
import java.util.Properties;
import javax.ws.rs.core.MediaType;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Carries application constants (such as version) and desired environmental settings (such as charset or locale).
 */
public final class Defaults {

    private static final Logger LOGGER = LogManager.getLogger(Defaults.class);

    public static final Currency CURRENCY = Currency.getInstance("CZK");
    public static final String MEDIA_TYPE = MediaType.APPLICATION_JSON + "; charset=UTF-8";
    public static final Locale LOCALE = Locale.forLanguageTag("cs-CZ");
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Prague");
    /**
     * May be null when running from IDE, Maven Surefire etc.; Maven resource filtering may not be applied there.
     */
    public static final String ROBOZONKY_VERSION = getVersion();
    public static final String ROBOZONKY_URL = "http://www.robozonky.cz";
    public static final String ROBOZONKY_USER_AGENT =
            "RoboZonky/" + Defaults.ROBOZONKY_VERSION + " (" + Defaults.ROBOZONKY_URL + ")";

    private Defaults() {
        // no instances
    }

    private static String getVersion() {
        try (final InputStream inputStream = Defaults.class.getResourceAsStream("version.properties")) {
            final Properties properties = new Properties();
            properties.load(inputStream);
            return properties.getProperty("version", "missing");
        } catch (final Exception ex) {
            LOGGER.debug("Failed reading RoboZonky version.", ex);
            return "unknown";
        }
    }
}
