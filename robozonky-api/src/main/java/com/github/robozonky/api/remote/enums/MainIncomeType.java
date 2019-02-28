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

@JsonDeserialize(using = MainIncomeType.MainIncomeTypeDeserializer.class)
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

    private static final Logger LOGGER = LogManager.getLogger(MainIncomeType.class);
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

    static class MainIncomeTypeDeserializer extends JsonDeserializer<MainIncomeType> {

        @Override
        public MainIncomeType deserialize(final JsonParser jsonParser,
                                          final DeserializationContext deserializationContext)
                throws IOException {
            final String id = jsonParser.getText();
            try {
                return MainIncomeType.valueOf(id);
            } catch (final Exception ex) {
                LOGGER.warn("Received unknown loan region from Zonky: '{}'. This may be a problem, but we continue.",
                            id);
                return OTHERS_MAIN;
            }
        }
    }

}
