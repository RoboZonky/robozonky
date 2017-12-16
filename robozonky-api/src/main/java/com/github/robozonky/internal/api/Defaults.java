/*
 * Copyright 2017 The RoboZonky Project
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

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;
import javax.ws.rs.core.MediaType;

/**
 * Carries application constants (such as version) and desired environmental settings (such as charset or locale).
 */
public final class Defaults {

    public static final String MEDIA_TYPE = MediaType.APPLICATION_JSON + "; charset=UTF-8";
    public static final Locale LOCALE = Locale.forLanguageTag("cs_CZ");
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Prague");
    /**
     * Small investors can only invest this amount into a single loan.
     */
    public static final int MINIMAL_MAXIMUM_INVESTMENT_IN_CZK = 5_000;
    /**
     * Biggest investors can invest this amount into a single loan.
     */
    public static final int MAXIMUM_INVESTMENT_IN_CZK = 20_000;
    public static final int MINIMUM_INVESTMENT_IN_CZK = 200;
    public static final int MINIMUM_INVESTMENT_INCREMENT_IN_CZK = 200;
    /**
     * Will be null when running from IDE, Maven Surefire etc.; no JAR information at the time.
     */
    public static final String ROBOZONKY_VERSION = Defaults.class.getPackage().getImplementationVersion();
    public static final String ROBOZONKY_URL = "http://www.robozonky.cz";
    public static final String ROBOZONKY_USER_AGENT =
            "RoboZonky/" + Defaults.ROBOZONKY_VERSION + " (" + Defaults.ROBOZONKY_URL + ")";
}
