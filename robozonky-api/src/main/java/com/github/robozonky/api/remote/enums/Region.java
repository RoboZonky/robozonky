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
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * {@link #UNKNOWN} must always come last - it is an internal value, not in the Zonky API, and therefore must only get
 * its integer ID after all other values already got one. Never change the value of {@link #getCode()}, as that will be
 * used throughout the strategies etc.
 */
@JsonDeserialize(using = Region.RegionDeserializer.class)
public enum Region implements BaseEnum {

    PRAHA("Praha", "Hlavní město Praha"),
    STREDOCESKY("Středočeský"),
    JIHOCESKY("Jihočeský"),
    PLZENSKY("Plzeňský"),
    KARLOVARSKY("Karlovarský"),
    USTECKY("Ústecký"),
    LIBERECKY("Liberecký"),
    KRALOVEHRADECKY("Královéhradecký"),
    PARDUBICKY("Pardubický"),
    VYSOCINA("Vysočina", "Kraj Vysočina"),
    JIHOMORAVSKY("Jihomoravský"),
    OLOMOUCKY("Olomoucký"),
    MORAVSKOSLEZSKY("Moravskoslezský"),
    ZLINSKY("Zlínský"),
    UNKNOWN("N/A");

    private static final Logger LOGGER = LogManager.getLogger();
    private final String code;
    private final String richCode;

    Region(final String code) {
        this(code, code + " kraj");
    }

    Region(final String code, final String richCode) {
        this.code = code;
        this.richCode = richCode;
    }

    public static Region findByCode(final String code) {
        return Stream.of(Region.values())
                .filter(r -> Objects.equals(r.code, code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown region: " + code));
    }

    @Override
    public String getCode() {
        return code;
    }

    /**
     * Purely for display purposes. It may seem unused from Java code, but may still be used from Freemarker templates.
     * @return
     */
    public String getRichCode() {
        return richCode;
    }

    static class RegionDeserializer extends JsonDeserializer<Region> {

        @Override
        public Region deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final String id = jsonParser.getText();
            try {
                final int actualId = Integer.parseInt(id) - 1; // regions in Zonky API are indexed from 1
                return Region.values()[actualId];
            } catch (final Exception ex) {
                LOGGER.warn("Received unknown loan region from Zonky: '{}'. This may be a problem, but we continue.",
                            id);
                return UNKNOWN;
            }
        }
    }
}
