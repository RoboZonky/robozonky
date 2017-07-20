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
import javax.ws.rs.core.MediaType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Carries application constants (such as version) and desired environmental settings (such as charset or locale).
 */
public final class Defaults {

    private static final Logger LOGGER = LoggerFactory.getLogger(Defaults.class);

    public static final String MEDIA_TYPE = MediaType.APPLICATION_JSON + ";charset=UTF-8";
    public static final Locale LOCALE = Locale.forLanguageTag("cs_CZ");
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Prague");
    public static final int MINIMUM_INVESTMENT_IN_CZK = 200;
    public static final int MINIMUM_INVESTMENT_INCREMENT_IN_CZK = 200;
    public static final String ROBOZONKY_VERSION = Defaults.class.getPackage().getImplementationVersion();
    public static final String ROBOZONKY_URL = "http://www.robozonky.cz";
    public static final String ROBOZONKY_USER_AGENT =
            "RoboZonky/" + Defaults.ROBOZONKY_VERSION + " (" + Defaults.ROBOZONKY_URL + ")";

}
