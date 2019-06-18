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

package com.github.robozonky.api.remote.enums;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@JsonDeserialize(using = Purpose.PurposeDeserializer.class)
public enum Purpose implements BaseEnum {

    AUTO_MOTO("auto-moto"),
    EDUCATION("vzdělání"),
    TRAVEL("cestování"),
    ELECTRONICS("elektronika"),
    HEALTH("zdraví"),
    REFINANCING("refinancování půjček"),
    HOUSEHOLD("domácnost"),
    OWN_PROJECT("vlastní projekt"),
    OTHER("jiné");

    private static final Logger LOGGER = LogManager.getLogger(Purpose.class);
    private final String code;

    Purpose(final String code) {
        this.code = code;
    }

    private static Optional<Purpose> maybeFindByCode(final String code) {
        return Stream.of(Purpose.values())
                .filter(r -> Objects.equals(r.code, code))
                .findFirst();
    }

    public static Purpose findByCode(final String code) {
        return maybeFindByCode(code).orElseThrow(() -> new IllegalArgumentException("Unknown loan purpose: " + code));
    }

    @Override
    public String getCode() {
        return code;
    }

    static class PurposeDeserializer extends JsonDeserializer<Purpose> {

        @Override
        public Purpose deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final String id = jsonParser.getText();
            try {
                return Purpose.valueOf(id);
            } catch (final Exception ex) {
                LOGGER.warn("Received unknown loan purpose from Zonky: '{}'. This may be a problem, but we continue.",
                            id);
                return OTHER;
            }
        }
    }
}
