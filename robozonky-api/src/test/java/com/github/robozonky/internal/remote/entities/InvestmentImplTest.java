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

package com.github.robozonky.internal.remote.entities;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;

import com.github.robozonky.api.remote.entities.Investment;

class InvestmentImplTest {

    private static final String PENDING_INVESTMENT_JSON = "{\"id\":14300760,\"loan\":{\"id\":769283," +
            "\"activeLoanOrdinal\":1,\"contractNo\":\"1009024644\",\"userNo\":\"1000421737\",\"title\":\"rekonstrukci" +
            " chalupy\",\"story\":\"Oprava chalupy po rodičích.\",\"annuity\":1925.00," +
            "\"detailLabels\":[\"CURRENTLY_INSURED\",\"VERIFIED_INCOME\",\"VERIFIED_BORROWER\"]," +
            "\"borrower\":{\"id\":732789,\"primaryIncomeType\":\"EMPLOYMENT\",\"region\":\"JIHOMORAVSKY\"}," +
            "\"healthStats\":{\"paidInstalments\":0,\"longestDaysDue\":1,\"currentDaysDue\":0," +
            "\"instalmentsCurrentlyInDue\":null,\"daysSinceLastInDue\":0,\"loanHealthInfo\":\"HEALTHY\"," +
            "\"dueInstalments\":0},\"purpose\":\"HOUSEHOLD\",\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\"," +
            "\"payments\":{\"total\":60,\"unpaid\":60},\"revenueRate\":0.0449,\"interestRate\":0.049900," +
            "\"hasCollectionHistory\":false,\"label\":\"PENDING\",\"nextPaymentDate\":\"2020-09-15\"}," +
            "\"smpSellInfo\":null,\"principal\":{\"total\":200.00,\"unpaid\":200.00},\"interest\":{\"total\":26.93," +
            "\"unpaid\":26.93},\"sellStatus\":\"NOT_SELLABLE\",\"timeCreated\":null}";
    private static final String FINISHED_INVESTMENT_JSON = "{\"id\":13214919,\"loan\":{\"id\":517589," +
            "\"activeLoanOrdinal\":0,\"contractNo\":\"1006111160\",\"userNo\":\"1000343137\"," +
            "\"title\":\"Refinancování půjček\",\"story\":\"Peníze použiji na zaplacení všech mých dosavadních " +
            "závazků, tak abych měl vše přehledně a stručně na jednom místě s jedním úrokem. Půjčku hodlám předčasně " +
            "splácen mimořádnými vklady. \",\"annuity\":6234.00,\"detailLabels\":[\"CURRENTLY_INSURED\"," +
            "\"VERIFIED_INCOME\",\"VERIFIED_BORROWER\"],\"borrower\":{\"id\":511107," +
            "\"primaryIncomeType\":\"EMPLOYMENT\",\"region\":\"JIHOMORAVSKY\"}," +
            "\"healthStats\":{\"paidInstalments\":13,\"longestDaysDue\":1,\"currentDaysDue\":0," +
            "\"instalmentsCurrentlyInDue\":null,\"daysSinceLastInDue\":0,\"loanHealthInfo\":\"HEALTHY\"," +
            "\"dueInstalments\":0},\"purpose\":\"REFINANCING\",\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\"," +
            "\"payments\":{\"total\":13,\"unpaid\":0},\"revenueRate\":0.0999,\"interestRate\":0.134900," +
            "\"hasCollectionHistory\":false,\"label\":null,\"nextPaymentDate\":null},\"smpSellInfo\":null," +
            "\"principal\":{\"total\":190.60,\"unpaid\":0.00},\"interest\":{\"total\":8.49,\"unpaid\":0.01}," +
            "\"sellStatus\":\"NOT_SELLABLE\",\"timeCreated\":null}";
    private static final String SOLD_INVESTMENT_JSON = "{\"id\":10232546,\"loan\":{\"id\":193181," +
            "\"activeLoanOrdinal\":0,\"contractNo\":\"1002394386\",\"userNo\":\"1000187368\",\"title\":\"SEN\"," +
            "\"story\":\"Dobrý den, ráda bych peníze použila na rozjezd podnikání,chtěla bych si splnit svůj sen a " +
            "otevřít malou Trafiku\",\"annuity\":3973.00,\"detailLabels\":[\"COVID_19_POSTPONEMENT_PROCESSED\"," +
            "\"VERIFIED_INCOME\",\"VERIFIED_BORROWER\"],\"borrower\":{\"id\":193181," +
            "\"primaryIncomeType\":\"SELF_EMPLOYMENT\",\"region\":\"STREDOCESKY\"}," +
            "\"healthStats\":{\"paidInstalments\":26,\"longestDaysDue\":1,\"currentDaysDue\":0," +
            "\"instalmentsCurrentlyInDue\":null,\"daysSinceLastInDue\":0,\"loanHealthInfo\":\"HEALTHY\"," +
            "\"dueInstalments\":0},\"purpose\":\"OTHER\",\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\"," +
            "\"payments\":{\"total\":61,\"unpaid\":34},\"revenueRate\":0.0449,\"interestRate\":0.049900," +
            "\"hasCollectionHistory\":false,\"label\":null,\"nextPaymentDate\":null},\"smpSellInfo\":null," +
            "\"principal\":{\"total\":122.23,\"unpaid\":118.90},\"interest\":{\"total\":9.29,\"unpaid\":8.81}," +
            "\"sellStatus\":\"SOLD\",\"timeCreated\":null}";
    private static final String DEFAULTED_INVESTMENT_JSON = "{\"id\":6997009,\"loan\":{\"id\":565277," +
            "\"activeLoanOrdinal\":0,\"contractNo\":\"1006652581\",\"userNo\":\"1000440547\"," +
            "\"title\":\"Refinancování půjček\",\"story\":\"zonky508708\",\"annuity\":2585.00," +
            "\"detailLabels\":[\"VERIFIED_INCOME\",\"VERIFIED_BORROWER\"],\"borrower\":{\"id\":552320," +
            "\"primaryIncomeType\":\"EMPLOYMENT\",\"region\":\"STREDOCESKY\"},\"healthStats\":{\"paidInstalments\":5," +
            "\"longestDaysDue\":147,\"currentDaysDue\":147,\"instalmentsCurrentlyInDue\":null," +
            "\"daysSinceLastInDue\":0,\"loanHealthInfo\":\"CURRENTLY_IN_DUE\",\"dueInstalments\":0}," +
            "\"purpose\":\"REFINANCING\",\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\",\"payments\":{\"total\":6," +
            "\"unpaid\":2},\"revenueRate\":0.0629,\"interestRate\":0.084900,\"hasCollectionHistory\":true," +
            "\"label\":\"TERMINATED\",\"nextPaymentDate\":null},\"smpSellInfo\":null,\"principal\":{\"total\":200.00," +
            "\"unpaid\":194.99},\"interest\":{\"total\":9.54,\"unpaid\":2.48},\"sellStatus\":\"NOT_SELLABLE\"," +
            "\"timeCreated\":null}";
    private static final String LATE_INVESTMENT_JSON = "{\"id\":10011582,\"loan\":{\"id\":685008," +
            "\"activeLoanOrdinal\":0,\"contractNo\":\"1008025194\",\"userNo\":\"10000517730\",\"title\":\"Auto \"," +
            "\"story\":\"zonky592794\",\"annuity\":5263.00,\"detailLabels\":[\"CURRENTLY_INSURED\"," +
            "\"VERIFIED_INCOME\",\"VERIFIED_BORROWER\"],\"borrower\":{\"id\":658178," +
            "\"primaryIncomeType\":\"EMPLOYMENT\",\"region\":\"PARDUBICKY\"},\"healthStats\":{\"paidInstalments\":3," +
            "\"longestDaysDue\":21,\"currentDaysDue\":21,\"instalmentsCurrentlyInDue\":null,\"daysSinceLastInDue\":0," +
            "\"loanHealthInfo\":\"CURRENTLY_IN_DUE\",\"dueInstalments\":0},\"purpose\":\"REFINANCING\"," +
            "\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\",\"payments\":{\"total\":84,\"unpaid\":81}," +
            "\"revenueRate\":0.0799,\"interestRate\":0.109900,\"hasCollectionHistory\":true," +
            "\"label\":\"PAST_DUE_CURRENTLY\",\"nextPaymentDate\":\"2020-07-24\"},\"smpSellInfo\":{\"boughtFor\":600" +
            ".00,\"remainingPrincipal\":593.85,\"discount\":0.11,\"fee\":{\"value\":7.93," +
            "\"expiresAt\":\"2021-03-09T00:00:00.000+01:00\"},\"sellPrice\":528.53},\"principal\":{\"total\":600.00," +
            "\"unpaid\":593.85},\"interest\":{\"total\":271.21,\"unpaid\":252.11}," +
            "\"sellStatus\":\"SELLABLE_WITH_FEE\",\"timeCreated\":null}";
    private static final String HISTORICALLY_LATE_INVESTMENT_JSON = "{\"id\":13803553,\"loan\":{\"id\":691603," +
            "\"activeLoanOrdinal\":0,\"contractNo\":\"1008099820\",\"userNo\":\"10000522084\",\"title\":\"Renovace a " +
            "imvestice\",\"story\":\"zonky597372\",\"annuity\":4404.00,\"detailLabels\":[\"CURRENTLY_INSURED\"," +
            "\"VERIFIED_INCOME\",\"VERIFIED_BORROWER\"],\"borrower\":{\"id\":664060," +
            "\"primaryIncomeType\":\"SELF_EMPLOYMENT\",\"region\":\"ZLINSKY\"}," +
            "\"healthStats\":{\"paidInstalments\":4,\"longestDaysDue\":5,\"currentDaysDue\":0," +
            "\"instalmentsCurrentlyInDue\":null,\"daysSinceLastInDue\":17,\"loanHealthInfo\":\"HISTORICALLY_IN_DUE\"," +
            "\"dueInstalments\":1},\"purpose\":\"AUTO_MOTO\",\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\"," +
            "\"payments\":{\"total\":66,\"unpaid\":62},\"revenueRate\":0.0999,\"interestRate\":0.134900," +
            "\"hasCollectionHistory\":true,\"label\":\"PAST_DUE_PREVIOUSLY\",\"nextPaymentDate\":\"2020-08-24\"}," +
            "\"smpSellInfo\":{\"boughtFor\":196.96,\"remainingPrincipal\":195.71,\"discount\":0.19," +
            "\"fee\":{\"value\":2.38,\"expiresAt\":\"2021-07-06T00:00:00.000+02:00\"},\"sellPrice\":158.53}," +
            "\"principal\":{\"total\":196.96,\"unpaid\":195.71},\"interest\":{\"total\":79.34,\"unpaid\":77.16}," +
            "\"sellStatus\":\"SELLABLE_WITH_FEE\",\"timeCreated\":null}";
    private static final String NEVER_LATE_INVESTMENT_JSON = "{\"id\":14284046,\"loan\":{\"id\":740849," +
            "\"activeLoanOrdinal\":0,\"contractNo\":\"1008689214\",\"userNo\":\"1000270186\"," +
            "\"title\":\"Refinancování půjček\",\"story\":\"zonky318590\",\"annuity\":8419.00," +
            "\"detailLabels\":[\"CURRENTLY_INSURED\",\"VERIFIED_INCOME\",\"VERIFIED_BORROWER\"]," +
            "\"borrower\":{\"id\":707295,\"primaryIncomeType\":\"EMPLOYMENT\",\"region\":\"STREDOCESKY\"}," +
            "\"healthStats\":{\"paidInstalments\":1,\"longestDaysDue\":1,\"currentDaysDue\":0," +
            "\"instalmentsCurrentlyInDue\":null,\"daysSinceLastInDue\":0,\"loanHealthInfo\":\"HEALTHY\"," +
            "\"dueInstalments\":0},\"purpose\":\"REFINANCING\",\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\"," +
            "\"payments\":{\"total\":84,\"unpaid\":83},\"revenueRate\":0.0999,\"interestRate\":0.134900," +
            "\"hasCollectionHistory\":false,\"label\":null,\"nextPaymentDate\":\"2020-09-10\"}," +
            "\"smpSellInfo\":{\"boughtFor\":199.66,\"remainingPrincipal\":199.66,\"discount\":0.00," +
            "\"fee\":{\"value\":2.99,\"expiresAt\":\"2021-08-13T00:00:00.000+02:00\"},\"sellPrice\":199.66}," +
            "\"principal\":{\"total\":199.66,\"unpaid\":199.66},\"interest\":{\"total\":110.08,\"unpaid\":110.08}," +
            "\"sellStatus\":\"SELLABLE_WITH_FEE\",\"timeCreated\":null}";

    @Test
    void deserializeNeverLate() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Investment investment = jsonb.fromJson(NEVER_LATE_INVESTMENT_JSON, InvestmentImpl.class);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(investment.getId())
                    .isEqualTo(12851390);
            });
        }
    }

}
