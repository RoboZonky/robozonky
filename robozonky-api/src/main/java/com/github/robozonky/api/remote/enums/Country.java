/*
 * Copyright 2020 The RoboZonky Project
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

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

/**
 * {@link #UNKNOWN} must always come last - it is an internal value, not in the Zonky API, and therefore must only get
 * its integer ID after all other values already got one. Never change the value of {@link #getCode()}, as that will be
 * used throughout the strategies etc.
 */
@JsonDeserialize(using = Country.CountryDeserializer.class)
public enum Country implements BaseEnum {

    CZECHIA("CZ"),
    UNKNOWN("N/A");

    private final String code;

    Country(final String code) {
        this.code = code;
    }

    public static Country findByCode(final String code) {
        return Stream.of(Country.values())
            .filter(r -> Objects.equals(r.code, code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown country: " + code));
    }

    @Override
    public String getCode() {
        return code;
    }

    static final class CountryDeserializer extends AbstractDeserializer<Country> {

        public CountryDeserializer() {
            super(Country::findByCode, UNKNOWN);
        }
    }
}
