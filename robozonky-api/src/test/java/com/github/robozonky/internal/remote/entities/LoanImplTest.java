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

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

class LoanImplTest {

    private static final String LOAN_JSON = "{\"id\":710354,\"url\":\"https://app.zonky.cz/loan/710354\"," +
            "\"name\":\"Ojeté auto\",\"story\":\"Potřebuji zánovní auto a vyplatit zbývající částku předchozího úvěru" +
            ".\",\"purpose\":\"REFINANCING\",\"photos\":[{\"name\":\"refinancing-5-82d5055f10774a57c5b3387029084b95" +
            ".jpg\",\"url\":\"/loans/710354/photos/90492\"}],\"userId\":150893,\"borrowerNo\":\"1000120468\"," +
            "\"publicIdentifier\":\"10001696613\",\"nickName\":\"zonky150893\",\"termInMonths\":60,\"interestRate\":0" +
            ".049900,\"revenueRate\":0.044900,\"annuity\":6921.00,\"premium\":615,\"rating\":\"AAAA\"," +
            "\"topped\":false,\"amount\":360000.00,\"currency\":\"CZK\",\"countryOfOrigin\":\"CZ\"," +
            "\"remainingInvestment\":318900.00,\"investmentRate\":0.11416666666666667,\"covered\":false," +
            "\"reservedAmount\":72000.00,\"zonkyPlusAmount\":4100.00,\"datePublished\":\"2020-04-21T19:23:46.278Z\"," +
            "\"published\":true,\"deadline\":\"2020-04-23T19:21:57.838Z\",\"investmentsCount\":6,\"region\":\"15\"," +
            "\"mainIncomeType\":\"EMPLOYMENT\",\"mainIncomeIndustry\":\"CONSTRUCTION\",\"activeLoansCount\":1," +
            "\"insuranceActive\":true,\"additionallyInsured\":false,\"flags\":[]," +
            "\"insuranceHistory\":[{\"policyPeriodFrom\":\"2020-04-22\",\"policyPeriodTo\":\"2025-04-13\"}]," +
            "\"myOtherInvestments\":null,\"borrowerRelatedInvestmentInfo\":null,\"annuityWithInsurance\":7536.00}";

    @Test
    void deserialize() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Loan loan = jsonb.fromJson(LOAN_JSON, LoanImpl.class);
            assertSoftly(softly -> {
                softly.assertThat(loan.getId())
                    .isEqualTo(710354);
                softly.assertThat(loan.getUrl())
                    .endsWith("/loan/710354");
                softly.assertThat(loan.getName())
                    .isNotBlank();
                softly.assertThat(loan.getStory())
                    .isNotBlank();
                softly.assertThat(loan.getTermInMonths())
                    .isEqualTo(60);
                softly.assertThat(loan.getInterestRate())
                    .isEqualTo(Rating.AAAA.getInterestRate());
                softly.assertThat(loan.getAmount())
                    .isEqualTo(Money.from(360_000));
                softly.assertThat(loan.getAnnuity())
                    .isEqualTo(Money.from(6_921));
                softly.assertThat(loan.getAnnuityWithInsurance())
                    .isEqualTo(Money.from(7_536));
                softly.assertThat(loan.getPremium())
                    .isEqualTo(Money.from(615));
                softly.assertThat(loan.getRating())
                    .isEqualTo(Rating.AAAA);
                softly.assertThat(loan.getRemainingInvestment())
                    .isEqualTo(Money.from(318_900));
                softly.assertThat(loan.getReservedAmount())
                    .isEqualTo(Money.from(72_000));
                softly.assertThat(loan.getZonkyPlusAmount())
                    .isEqualTo(Money.from(4_100));
                softly.assertThat(loan.getRegion())
                    .isEqualTo(Region.SLOVENSKO);
                softly.assertThat(loan.getMainIncomeType())
                    .isEqualTo(MainIncomeType.EMPLOYMENT);
                softly.assertThat(loan.isInsuranceActive())
                    .isTrue();
            });
        }
    }

}
