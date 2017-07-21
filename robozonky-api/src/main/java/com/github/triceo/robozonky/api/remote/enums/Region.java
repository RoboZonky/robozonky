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

package com.github.triceo.robozonky.api.remote.enums;

import java.io.IOException;
import java.util.Objects;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * {@link #UNKNOWN} must always come last - it is an internal value, not in the Zonky API, and therefore must only get
 * its integer ID after all other values already got one.
 */
@JsonDeserialize(using = Region.RegionDeserializer.class)
public enum Region implements BaseEnum {

    PRAHA("Praha"),
    STREDOCESKY("Středočeský"),
    JIHOCESKY("Jihočeský"),
    PLZENSKY("Plzeňský"),
    KARLOVARSKY("Karlovarský"),
    USTECKY("Ústecký"),
    LIBERECKY("Liberecký"),
    KRALOVEHRADECKY("Královéhradecký"),
    PARDUBICKY("Pardubický"),
    VYSOCINA("Vysočina"),
    JIHOMORAVSKY("Jihomoravský"),
    OLOMOUCKY("Olomoucký"),
    MORAVSKOSLEZSKY("Moravskoslezský"),
    ZLINSKY("Zlínský"),
    UNKNOWN("N/A");

    static class RegionDeserializer extends JsonDeserializer<Region> {

        @Override
        public Region deserialize(final JsonParser jsonParser, final DeserializationContext deserializationContext)
                throws IOException {
            final String id = jsonParser.getText();
            final int actualId = Integer.parseInt(id) - 1; // regions in Zonky API are indexed from 1
            return Region.values()[actualId];
        }
    }

    public static Region findByCode(final String code) {
        return Stream.of(Region.values())
                .filter(r -> Objects.equals(r.code, code))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown region: " + code));
    }

    private final String code;

    Region(final String code) {
        this.code = code;
    }

    @Override
    public String getCode() {
        return code;
    }

}
