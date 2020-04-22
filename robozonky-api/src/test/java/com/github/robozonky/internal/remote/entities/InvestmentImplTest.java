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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.enums.InsuranceStatus;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

class InvestmentImplTest {

    private static final String INVESTMENT_JSON = "{\"id\":12851390,\"loanId\":311149,\"loanName\":\"Oprava vozu " +
            "\",\"investmentDate\":null,\"amount\":200.00,\"firstAmount\":200.00,\"purchasePrice\":143.70," +
            "\"interestRate\":0.109900,\"revenueRate\":0.0799,\"nickname\":\"zonky320118\",\"firstName\":null," +
            "\"surname\":null,\"rating\":\"A\",\"paid\":null,\"toPay\":null,\"nextPaymentDate\":\"2020-05-22T00:00:00" +
            ".000+02:00\",\"paymentStatus\":\"OK\",\"legalDpd\":0,\"amountDue\":null,\"loanTermInMonth\":54," +
            "\"paidInterest\":0.00,\"dueInterest\":0.00,\"paidPrincipal\":0.00,\"duePrincipal\":0.00," +
            "\"remainingPrincipal\":143.70,\"paidPenalty\":0.00,\"smpSoldFor\":null,\"expectedInterest\":25.63," +
            "\"smpPrice\":143.70,\"currentTerm\":54,\"canBeOffered\":true,\"onSmp\":false," +
            "\"insuranceStatus\":\"NOT_INSURED\",\"inWithdrawal\":false,\"smpRelated\":null," +
            "\"smpFeeExpirationDate\":\"2021-04-21T00:00:00.000+02:00\",\"investmentType\":\"N\"," +
            "\"currency\":\"CZK\",\"loanHealthInfo\":\"HEALTHY\",\"loanHealthStats\":null," +
            "\"borrowerNo\":\"1000271517\",\"loanPublicIdentifier\":\"1129000\",\"hasCollectionHistory\":true," +
            "\"remainingMonths\":36,\"status\":\"ACTIVE\",\"timeCreated\":null,\"activeFrom\":\"2020-04-21T21:51:29" +
            ".047+02:00\",\"activeTo\":null,\"smpFee\":2.16,\"insuranceActive\":false," +
            "\"instalmentPostponement\":false,\"insuranceHistory\":[],\"additionallyInsured\":false," +
            "\"loanInvestmentsCount\":68,\"loanAnnuity\":942.00,\"loanAmount\":40000.00,\"additionalAmount\":0.00}";

    @Test
    void deserialize() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Investment investment = jsonb.fromJson(INVESTMENT_JSON, InvestmentImpl.class);
            SoftAssertions.assertSoftly(softly -> {
                softly.assertThat(investment.getId()).isEqualTo(12851390);
                softly.assertThat(investment.getLoanId()).isEqualTo(311149);
                softly.assertThat(investment.getLoanName()).isNotBlank();
                softly.assertThat(investment.getInvestmentDate()).isNotNull();
                softly.assertThat(investment.getAmount()).isEqualTo(Money.from(200));
                softly.assertThat(investment.getPurchasePrice()).isEqualTo(Money.from(143.7));
                softly.assertThat(investment.getRating()).isEqualTo(Rating.A);
                softly.assertThat(investment.getInterestRate()).isEqualTo(Rating.A.getInterestRate());
                softly.assertThat(investment.getRevenueRate()).hasValue(Rating.A.getMaximalRevenueRate());
                softly.assertThat(investment.getLoanHealthInfo()).hasValue(LoanHealth.HEALTHY);
                softly.assertThat(investment.getPaymentStatus()).hasValue(PaymentStatus.OK);
                softly.assertThat(investment.getInsuranceStatus()).isEqualTo(InsuranceStatus.NOT_INSURED);
                softly.assertThat(investment.getStatus()).isEqualTo(InvestmentStatus.ACTIVE);
            });
        }
    }

}
