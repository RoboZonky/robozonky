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

package com.github.triceo.robozonky.api;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.util.Locale;

/**
 * Carries default values for some basic application properties, such as charset or locale.
 */
public class Defaults {

    public static final Locale LOCALE = Locale.forLanguageTag("cs_CZ");
    public static final Charset CHARSET = StandardCharsets.UTF_8;
    public static final ZoneId ZONE_ID = ZoneId.of("Europe/Prague");
    public static final int MINIMUM_INVESTMENT_IN_CZK = 200;
    public static final int MINIMUM_INVESTMENT_INCREMENT_IN_CZK = 200;


}
