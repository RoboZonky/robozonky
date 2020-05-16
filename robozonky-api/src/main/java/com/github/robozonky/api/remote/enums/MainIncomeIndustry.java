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

    AUTOMOTIVE_INDUSTRY("Automobilový průmysl"),
    TRANSPORTATION_AND_STORAGE("Doprava a skladování"),
    IT("IT"),
    FINANCIAL_AND_INSURANCE_ACTIVITIES("Peněžnictví a pojišťovnictví"),
    SERVICE_ACTIVITIES("Služby"),
    CONSTRUCTION("Stavebnictví"),
    TELECOMMUNICATION_MEDIA("Telekomunikace, Média"),
    ACCOMMODATION_AND_FOOD_SERVICE_ACTIVITIES("Ubytování, stravování a pohostinství"),
    WHOLESALE_AND_RETAIL_TRADE("Velkoobchod a maloobchod"),
    PUBLIC_ADMINISTRATION_AND_DEFENCE_COMPULSORY_SOCIAL_SECURITY("Veřejná správa a obrana; povin. sociál. zabezpečení"),
    ELECTRICITY_GAS_STEAM_AND_AIR_CONDITIONING_SUPPLY("Výroba a rozvod elektřiny, plynu, tepla"),
    EDUCATION("Vzdělávání"),
    HUMAN_HEALTH_AND_SOCIAL_WORK_ACTIVITIES("Zdravotní a sociální péče"),
    AGRICULTURE_FORESTRY_AND_FISHING("Zemědělství, lesnictví a rybářství"),
    MANUFACTURING("Zpracovatelský průmysl"),
    OTHER_SERVICE_ACTIVITIES("Jiné"),
    UNKNOWN("Neznámý obor");

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
