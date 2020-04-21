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

@JsonbTypeDeserializer(MainIncomeType.MainIncomeTypeDeserializer.class)
public enum MainIncomeType implements BaseEnum {

    EMPLOYMENT("zaměstnanec"),
    ENTREPRENEUR("podnikatel"),
    SELF_EMPLOYMENT("OSVČ"),
    PENSION("důchodce"),
    MATERNITY_LEAVE("na rodičovské dovolené"),
    STUDENT("student"),
    UNEMPLOYED("bez zaměstnání"),
    LIBERAL_PROFESSION("svobodné povolání"),
    RENT("rentiér"),
    OTHERS_MAIN("jiné");

    private final String code;

    MainIncomeType(final String code) {
        this.code = code;
    }

    public static MainIncomeType findByCode(final String code) {
        return Stream.of(MainIncomeType.values())
            .filter(r -> Objects.equals(r.code, code))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown main income type: " + code));
    }

    @Override
    public String getCode() {
        return code;
    }

    public static final class MainIncomeTypeDeserializer extends AbstractDeserializer<MainIncomeType> {

        public MainIncomeTypeDeserializer() {
            super(MainIncomeType::valueOf, OTHERS_MAIN);
        }

    }

}
