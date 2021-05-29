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

import jakarta.json.bind.annotation.JsonbTypeDeserializer;

import com.github.robozonky.internal.util.json.MainIncomeIndustryDeserializer;

/**
 * See <a href="http://www.nace.cz/">NACE</a>.
 */
@JsonbTypeDeserializer(MainIncomeIndustryDeserializer.class)
public enum MainIncomeIndustry implements BaseEnum {

    AGRICULTURE_FORESTRY_AND_FISHING("Zemědělství, lesnictví, rybářství"),
    MINING_AND_QUARRYING("Těžba a dobývání"),
    MANUFACTURING("Zpracovatelský průmysl"),
    ELECTRICITY_GAS_STEAM_AND_AIR_CONDITIONING_SUPPLY(
            "Výroba a rozvod elektřiny, plynu, tepla a klimatizovaného vzduchu"),
    WATER_SUPPLY_AND_WASTE_MANAGEMENT("Zásobování vodou; činnosti související s odpadními vodami, odpady a sanacemi"),
    CONSTRUCTION("Stavebnictví"),
    WHOLESALE_AND_RETAIL_TRADE_AND_VEHICLE_REPAIR("Velkoobchod a maloobchod; opravy a údržba motorových vozidel"),
    TRANSPORTATION_AND_STORAGE("Doprava a skladování"),
    ACCOMMODATION_AND_FOOD_SERVICE_ACTIVITIES("Ubytování, stravování a pohostinství"),
    IT_AND_TELCO("Informační a komunikační činnosti"),
    FINANCIAL_AND_INSURANCE_ACTIVITIES("Peněžnictví a pojišťovnictví"),
    REAL_ESTATE_ACTIVITIES("Činnosti v oblasti nemovitostí"),
    SCIENTIFIC_AND_TECHNICAL_ACTIVITIES("Profesní, vědecké a technické činnosti"),
    ADMINISTRATIVE_AND_SUPPORT_SERVICE_ACTIVITIES("Administrativní a podpůrné činnosti"),
    PUBLIC_ADMINISTRATION_AND_DEFENCE_COMPULSORY_SOCIAL_SECURITY(
            "Veřejná správa a obrana; povinné sociální zabezpečení"),
    EDUCATION("Vzdělávání"),
    HUMAN_HEALTH_AND_SOCIAL_WORK_ACTIVITIES("Zdravotní a sociální péče"),
    CULTURAL_ENTERTAINMENT_AND_RECREATIONAL_ACTIVITIES("Kulturní, zábavní a rekreační činnosti"),
    OTHER_SERVICE_ACTIVITIES("Ostatní činnosti"),
    HOUSEHOLD_ACTIVITIES(
            "Činnosti domácností jako zaměstnavatelů; činnosti domácností produkujících blíže neurčené výrobky a služby pro vlastní potřebu"),
    EXTRATERRITORIAL_ORGANIZATIONS_ACTIVITIES("Činnosti exteritoriálních organizací a orgánů"),
    UNKNOWN("Neznámé");

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
