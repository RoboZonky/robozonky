/*
 * Copyright 2021 The RoboZonky Project
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

import java.util.Objects;
import java.util.stream.Stream;

import javax.json.bind.annotation.JsonbTypeDeserializer;

import com.github.robozonky.internal.util.json.RegionDeserializer;

/**
 * {@link #UNKNOWN} must always come last - it is an internal value, not in the Zonky API, and therefore must only get
 * its integer ID after all other values already got one. Never change the value of {@link #getCode()}, as that will be
 * used throughout the strategies etc.
 */
@JsonbTypeDeserializer(RegionDeserializer.class)
public enum Region implements BaseEnum {

    HLAVNI_MESTO_PRAHA("Praha", "Hlavní město Praha"),
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
    SLOVENSKO("Slovensko", "Slovensko"),
    UNKNOWN("N/A");

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
     * 
     * @return never null
     */
    public String getRichCode() {
        return richCode;
    }
}
