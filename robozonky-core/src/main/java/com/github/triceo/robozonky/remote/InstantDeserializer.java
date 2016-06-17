/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.remote;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;

class InstantDeserializer extends JsonDeserializer<Instant> {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
    private static final Pattern COMPILE = Pattern.compile("([+\\-][0-9][0-9]):([0-9][0-9])");

    @Override
    public Instant deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext) throws IOException {
        try {
            final String dateText = jsonParser.getText();
            final Matcher m = InstantDeserializer.COMPILE.matcher(dateText); // +02:00 needs to become +0200
            m.matches();
            final String newDateText = m.replaceFirst("$1$2");
            return InstantDeserializer.DATE_FORMAT.parse(newDateText).toInstant();
        } catch (final ParseException e) {
            throw new IOException(e);
        }
    }

}
