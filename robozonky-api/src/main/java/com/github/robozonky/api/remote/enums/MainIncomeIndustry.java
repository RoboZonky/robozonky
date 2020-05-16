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

import javax.json.bind.annotation.JsonbTypeDeserializer;

import com.github.robozonky.internal.util.json.MainIncomeIndustryDeserializer;

@JsonbTypeDeserializer(MainIncomeIndustryDeserializer.class)
public enum MainIncomeIndustry implements BaseEnum {

    UNKNOWN("Neznámý");

    private final String code;

    MainIncomeIndustry(String code) {
        this.code = code;
    }

    public static MainIncomeIndustry findByCode(final String code) {
        return Stream.of(MainIncomeIndustry.values())
            .filter(i -> Objects.equals(i.code, code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown main income industry: " + code));
    }

    @Override
    public String getCode() {
        return code;
    }
}
