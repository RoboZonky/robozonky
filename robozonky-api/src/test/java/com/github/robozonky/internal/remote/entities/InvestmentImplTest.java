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

package com.github.robozonky.internal.remote.entities;

import static com.github.robozonky.api.remote.enums.DetailLabel.CURRENTLY_INSURED;
import static com.github.robozonky.api.remote.enums.DetailLabel.VERIFIED_BORROWER;
import static com.github.robozonky.api.remote.enums.DetailLabel.VERIFIED_INCOME;
import static com.github.robozonky.api.remote.enums.Label.PAST_DUE_CURRENTLY;
import static com.github.robozonky.api.remote.enums.Label.PAST_DUE_PREVIOUSLY;
import static com.github.robozonky.api.remote.enums.Label.PENDING;
import static com.github.robozonky.api.remote.enums.Label.TERMINATED;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.Optional;
import java.util.OptionalInt;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;

import org.junit.jupiter.api.Test;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.InvestmentLoanData;
import com.github.robozonky.api.remote.entities.LoanHealthStats;
import com.github.robozonky.api.remote.entities.SellFee;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.remote.enums.SellStatus;

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
    private static final String FINISHED_INVESTMENT_JSON = "{\"id\":10232542,\"principal\":{\"total\":101.47,\"unpaid\":0"
            +
            ".00},\"interest\":{\"total\":1.64,\"unpaid\":0.00},\"sellStatus\":\"NOT_SELLABLE\"," +
            "\"loan\":{\"id\":385418,\"title\":\"Refinancování úvěru\",\"purpose\":\"REFINANCING\"," +
            "\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\",\"payments\":{\"total\":19,\"unpaid\":0}," +
            "\"revenueRate\":0.0449,\"interestRate\":0.049900,\"hasCollectionHistory\":false,\"dpd\":0," +
            "\"label\":null,\"nextPaymentDate\":null,\"borrower\":{\"region\":\"JIHOMORAVSKY\"}}," +
            "\"isBlockedByAB4\":false}";
    private static final String SOLD_INVESTMENT_JSON = "{\"id\":10083108,\"principal\":{\"total\":72.67,\"unpaid\":72" +
            ".67},\"interest\":{\"total\":8.69,\"unpaid\":8.69},\"sellStatus\":\"SOLD\",\"loan\":{\"id\":40056," +
            "\"title\":\"Zdraví\",\"purpose\":\"HEALTH\",\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\"," +
            "\"payments\":{\"total\":60,\"unpaid\":17},\"revenueRate\":0.1149,\"interestRate\":0.154900," +
            "\"hasCollectionHistory\":false,\"dpd\":0,\"label\":null,\"nextPaymentDate\":null," +
            "\"borrower\":{\"region\":\"USTECKY\"}},\"isBlockedByAB4\":false}";
    private static final String DEFAULTED_INVESTMENT_JSON = "{\"id\":3042483,\"principal\":{\"total\":188.23," +
            "\"unpaid\":160.32},\"interest\":{\"total\":27.74,\"unpaid\":7.94},\"sellStatus\":\"NOT_SELLABLE\"," +
            "\"loan\":{\"id\":204677,\"title\":\"Prosba o finanční pomoc\",\"purpose\":\"REFINANCING\"," +
            "\"countryOfOrigin\":\"CZ\",\"currency\":\"CZK\",\"payments\":{\"total\":20,\"unpaid\":5}," +
            "\"revenueRate\":0.0999,\"interestRate\":0.134900,\"hasCollectionHistory\":true,\"dpd\":412," +
            "\"label\":\"TERMINATED\",\"nextPaymentDate\":null,\"borrower\":{\"region\":\"HLAVNI_MESTO_PRAHA\"}}," +
            "\"isBlockedByAB4\":false}";
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
            assertSoftly(softly -> {
                softly.assertThat(investment.getId())
                    .isEqualTo(14284046);
                softly.assertThat(investment.getSellStatus())
                    .isEqualTo(SellStatus.SELLABLE_WITH_FEE);
                softly.assertThat(investment.getPrincipal())
                    .isEqualTo(new AmountsImpl(Money.from(199.66)));
                softly.assertThat(investment.getInterest())
                    .isEqualTo(new AmountsImpl(Money.from(110.08)));
                // SellInfo is present and contains particular values.
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getFee)
                    .map(SellFee::getValue)
                    .contains(Money.from(2.99));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getBoughtFor)
                    .contains(Money.from(199.66));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getDiscount)
                    .contains(Ratio.ZERO);
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getRemainingPrincipal)
                    .contains(Money.from(199.66));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getSellPrice)
                    .contains(Money.from(199.66));
                // Loan data is present and contains particular values
                softly.assertThat(investment.getLoan())
                    .returns(740849, InvestmentLoanData::getId)
                    .returns(0, InvestmentLoanData::getDpd)
                    .returns(false, InvestmentLoanData::hasCollectionHistory)
                    .returns(Optional.of(Money.from(8419)), InvestmentLoanData::getAnnuity)
                    .returns(new InstalmentsImpl(84, 83), InvestmentLoanData::getPayments)
                    .returns(Purpose.REFINANCING, InvestmentLoanData::getPurpose)
                    .returns(Optional.of(MainIncomeType.EMPLOYMENT), l -> l.getBorrower()
                        .getPrimaryIncomeType())
                    .returns(Region.STREDOCESKY, l -> l.getBorrower()
                        .getRegion())
                    .returns(Ratio.fromPercentage("9.99"), InvestmentLoanData::getRevenueRate)
                    .returns(Ratio.fromPercentage("13.49"), InvestmentLoanData::getInterestRate);
                softly.assertThat(investment.getLoan()
                    .getLabel())
                    .isEmpty();
                softly.assertThat(investment.getLoan()
                    .getDetailLabels())
                    .containsOnly(CURRENTLY_INSURED, VERIFIED_BORROWER, VERIFIED_INCOME);
                softly.assertThat(investment.getLoan()
                    .getStory())
                    .isNotEmpty();
                softly.assertThat(investment.getLoan()
                    .getTitle())
                    .isNotBlank();
                // Health stats are present and contain particular values
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getInstalmentsCurrentlyInDue)
                    .contains(OptionalInt.empty());
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getLoanHealthInfo)
                    .contains(LoanHealth.HEALTHY);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getInstalmentsCurrentlyInDue)
                    .contains(OptionalInt.empty());
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getDaysSinceLastInDue)
                    .contains(0);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getDueInstalments)
                    .contains(0);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getLongestDaysDue)
                    .contains(1);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getPaidInstalments)
                    .contains(1);
            });
        }
    }

    @Test
    void deserializeHistoricallyLate() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Investment investment = jsonb.fromJson(HISTORICALLY_LATE_INVESTMENT_JSON, InvestmentImpl.class);
            assertSoftly(softly -> {
                softly.assertThat(investment.getId())
                    .isEqualTo(13803553);
                softly.assertThat(investment.getSellStatus())
                    .isEqualTo(SellStatus.SELLABLE_WITH_FEE);
                softly.assertThat(investment.getPrincipal())
                    .isEqualTo(new AmountsImpl(Money.from(196.96), Money.from(195.71)));
                softly.assertThat(investment.getInterest())
                    .isEqualTo(new AmountsImpl(Money.from(79.34), Money.from(77.16)));
                // SellInfo is present and contains particular values.
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getFee)
                    .map(SellFee::getValue)
                    .contains(Money.from(2.38));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getBoughtFor)
                    .contains(Money.from(196.96));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getDiscount)
                    .contains(Ratio.fromPercentage(19));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getRemainingPrincipal)
                    .contains(Money.from(195.71));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getSellPrice)
                    .contains(Money.from(158.53));
                // Loan data is present and contains particular values
                softly.assertThat(investment.getLoan())
                    .returns(691603, InvestmentLoanData::getId)
                    .returns(0, InvestmentLoanData::getDpd)
                    .returns(true, InvestmentLoanData::hasCollectionHistory)
                    .returns(Optional.of(Money.from(4404)), InvestmentLoanData::getAnnuity)
                    .returns(new InstalmentsImpl(66, 62), InvestmentLoanData::getPayments)
                    .returns(Purpose.AUTO_MOTO, InvestmentLoanData::getPurpose)
                    .returns(Optional.of(MainIncomeType.SELF_EMPLOYMENT), l -> l.getBorrower()
                        .getPrimaryIncomeType())
                    .returns(Region.ZLINSKY, l -> l.getBorrower()
                        .getRegion())
                    .returns(Ratio.fromPercentage("9.99"), InvestmentLoanData::getRevenueRate)
                    .returns(Ratio.fromPercentage("13.49"), InvestmentLoanData::getInterestRate);
                softly.assertThat(investment.getLoan()
                    .getLabel())
                    .contains(PAST_DUE_PREVIOUSLY);
                softly.assertThat(investment.getLoan()
                    .getDetailLabels())
                    .containsOnly(CURRENTLY_INSURED, VERIFIED_BORROWER, VERIFIED_INCOME);
                softly.assertThat(investment.getLoan()
                    .getStory())
                    .isNotEmpty();
                softly.assertThat(investment.getLoan()
                    .getTitle())
                    .isNotBlank();
                // Health stats are present and contain particular values
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getInstalmentsCurrentlyInDue)
                    .contains(OptionalInt.empty());
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getLoanHealthInfo)
                    .contains(LoanHealth.HISTORICALLY_IN_DUE);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getInstalmentsCurrentlyInDue)
                    .contains(OptionalInt.empty());
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getDaysSinceLastInDue)
                    .contains(17);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getDueInstalments)
                    .contains(1);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getLongestDaysDue)
                    .contains(5);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getPaidInstalments)
                    .contains(4);
            });
        }
    }

    @Test
    void deserializeLate() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Investment investment = jsonb.fromJson(LATE_INVESTMENT_JSON, InvestmentImpl.class);
            assertSoftly(softly -> {
                softly.assertThat(investment.getId())
                    .isEqualTo(10011582);
                softly.assertThat(investment.getSellStatus())
                    .isEqualTo(SellStatus.SELLABLE_WITH_FEE);
                softly.assertThat(investment.getPrincipal())
                    .isEqualTo(new AmountsImpl(Money.from(600), Money.from(593.85)));
                softly.assertThat(investment.getInterest())
                    .isEqualTo(new AmountsImpl(Money.from(271.21), Money.from(252.11)));
                // SellInfo is present and contains particular values.
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getFee)
                    .map(SellFee::getValue)
                    .contains(Money.from(7.93));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getBoughtFor)
                    .contains(Money.from(600));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getDiscount)
                    .contains(Ratio.fromPercentage(11));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getRemainingPrincipal)
                    .contains(Money.from(593.85));
                softly.assertThat(investment.getSmpSellInfo())
                    .map(SellInfo::getSellPrice)
                    .contains(Money.from(528.53));
                // Loan data is present and contains particular values
                softly.assertThat(investment.getLoan())
                    .returns(685008, InvestmentLoanData::getId)
                    .returns(0, InvestmentLoanData::getDpd)
                    .returns(true, InvestmentLoanData::hasCollectionHistory)
                    .returns(Optional.of(Money.from(5263)), InvestmentLoanData::getAnnuity)
                    .returns(new InstalmentsImpl(84, 81), InvestmentLoanData::getPayments)
                    .returns(Purpose.REFINANCING, InvestmentLoanData::getPurpose)
                    .returns(Optional.of(MainIncomeType.EMPLOYMENT), l -> l.getBorrower()
                        .getPrimaryIncomeType())
                    .returns(Region.PARDUBICKY, l -> l.getBorrower()
                        .getRegion())
                    .returns(Ratio.fromPercentage("7.99"), InvestmentLoanData::getRevenueRate)
                    .returns(Ratio.fromPercentage("10.99"), InvestmentLoanData::getInterestRate);
                softly.assertThat(investment.getLoan()
                    .getLabel())
                    .contains(PAST_DUE_CURRENTLY);
                softly.assertThat(investment.getLoan()
                    .getDetailLabels())
                    .containsOnly(CURRENTLY_INSURED, VERIFIED_BORROWER, VERIFIED_INCOME);
                softly.assertThat(investment.getLoan()
                    .getStory())
                    .isNotEmpty();
                softly.assertThat(investment.getLoan()
                    .getTitle())
                    .isNotBlank();
                // Health stats are present and contain particular values
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getInstalmentsCurrentlyInDue)
                    .contains(OptionalInt.empty());
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getLoanHealthInfo)
                    .contains(LoanHealth.CURRENTLY_IN_DUE);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getInstalmentsCurrentlyInDue)
                    .contains(OptionalInt.empty());
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getDaysSinceLastInDue)
                    .contains(0);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getDueInstalments)
                    .contains(0);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getLongestDaysDue)
                    .contains(21);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getPaidInstalments)
                    .contains(3);
            });
        }
    }

    @Test
    void deserializeDefaulted() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Investment investment = jsonb.fromJson(DEFAULTED_INVESTMENT_JSON, InvestmentImpl.class);
            assertSoftly(softly -> {
                softly.assertThat(investment.getId())
                    .isEqualTo(3042483L);
                softly.assertThat(investment.getSellStatus())
                    .isEqualTo(SellStatus.NOT_SELLABLE);
                softly.assertThat(investment.getPrincipal())
                    .isEqualTo(new AmountsImpl(Money.from(188.23), Money.from(160.32)));
                softly.assertThat(investment.getInterest())
                    .isEqualTo(new AmountsImpl(Money.from(27.74), Money.from(7.94)));
                // SellInfo is missing.
                softly.assertThat(investment.getSmpSellInfo())
                    .isEmpty();
                // Loan data is present and contains particular values
                softly.assertThat(investment.getLoan())
                    .returns(204677, InvestmentLoanData::getId)
                    .returns(412, InvestmentLoanData::getDpd)
                    .returns(true, InvestmentLoanData::hasCollectionHistory)
                    .returns(Optional.empty(), InvestmentLoanData::getAnnuity)
                    .returns(new InstalmentsImpl(20, 5), InvestmentLoanData::getPayments)
                    .returns(Purpose.REFINANCING, InvestmentLoanData::getPurpose)
                    .returns(Optional.empty(), l -> l.getBorrower()
                        .getPrimaryIncomeType())
                    .returns(Region.HLAVNI_MESTO_PRAHA, l -> l.getBorrower()
                        .getRegion())
                    .returns(Ratio.fromPercentage("9.99"), InvestmentLoanData::getRevenueRate)
                    .returns(Ratio.fromPercentage("13.49"), InvestmentLoanData::getInterestRate);
                softly.assertThat(investment.getLoan()
                    .getLabel())
                    .contains(TERMINATED);
                softly.assertThat(investment.getLoan()
                    .getDetailLabels())
                    .isEmpty();
                softly.assertThat(investment.getLoan()
                    .getStory())
                    .isEmpty();
                softly.assertThat(investment.getLoan()
                    .getTitle())
                    .isNotBlank();
                // Health stats are missing.
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .isEmpty();
            });
        }
    }

    @Test
    void deserializeSold() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Investment investment = jsonb.fromJson(SOLD_INVESTMENT_JSON, InvestmentImpl.class);
            assertSoftly(softly -> {
                softly.assertThat(investment.getId())
                    .isEqualTo(10083108);
                softly.assertThat(investment.getSellStatus())
                    .isEqualTo(SellStatus.SOLD);
                softly.assertThat(investment.getPrincipal())
                    .isEqualTo(new AmountsImpl(Money.from(72.67)));
                softly.assertThat(investment.getInterest())
                    .isEqualTo(new AmountsImpl(Money.from(8.69)));
                // SellInfo is missing.
                softly.assertThat(investment.getSmpSellInfo())
                    .isEmpty();
                // Loan data is present and contains particular values
                softly.assertThat(investment.getLoan())
                    .returns(40056, InvestmentLoanData::getId)
                    .returns(0, InvestmentLoanData::getDpd)
                    .returns(false, InvestmentLoanData::hasCollectionHistory)
                    .returns(Optional.empty(), InvestmentLoanData::getAnnuity)
                    .returns(new InstalmentsImpl(60, 17), InvestmentLoanData::getPayments)
                    .returns(Purpose.HEALTH, InvestmentLoanData::getPurpose)
                    .returns(Optional.empty(), l -> l.getBorrower()
                        .getPrimaryIncomeType())
                    .returns(Region.USTECKY, l -> l.getBorrower()
                        .getRegion())
                    .returns(Ratio.fromPercentage("11.49"), InvestmentLoanData::getRevenueRate)
                    .returns(Ratio.fromPercentage("15.49"), InvestmentLoanData::getInterestRate);
                softly.assertThat(investment.getLoan()
                    .getLabel())
                    .isEmpty();
                softly.assertThat(investment.getLoan()
                    .getDetailLabels())
                    .isEmpty();
                softly.assertThat(investment.getLoan()
                    .getStory())
                    .isEmpty();
                softly.assertThat(investment.getLoan()
                    .getTitle())
                    .isNotBlank();
                // Health stats are missing.
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .isEmpty();
            });
        }
    }

    @Test
    void deserializeFinished() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Investment investment = jsonb.fromJson(FINISHED_INVESTMENT_JSON, InvestmentImpl.class);
            assertSoftly(softly -> {
                softly.assertThat(investment.getId())
                    .isEqualTo(10232542);
                softly.assertThat(investment.getSellStatus())
                    .isEqualTo(SellStatus.NOT_SELLABLE);
                softly.assertThat(investment.getPrincipal())
                    .isEqualTo(new AmountsImpl(Money.from(101.47), Money.ZERO));
                softly.assertThat(investment.getInterest())
                    .isEqualTo(new AmountsImpl(Money.from(1.64), Money.ZERO));
                // SellInfo is missing.
                softly.assertThat(investment.getSmpSellInfo())
                    .isEmpty();
                // Loan data is present and contains particular values
                softly.assertThat(investment.getLoan())
                    .returns(385418, InvestmentLoanData::getId)
                    .returns(0, InvestmentLoanData::getDpd)
                    .returns(false, InvestmentLoanData::hasCollectionHistory)
                    .returns(Optional.empty(), InvestmentLoanData::getAnnuity)
                    .returns(new InstalmentsImpl(19, 0), InvestmentLoanData::getPayments)
                    .returns(Purpose.REFINANCING, InvestmentLoanData::getPurpose)
                    .returns(Optional.empty(), l -> l.getBorrower()
                        .getPrimaryIncomeType())
                    .returns(Region.JIHOMORAVSKY, l -> l.getBorrower()
                        .getRegion())
                    .returns(Ratio.fromPercentage("4.49"), InvestmentLoanData::getRevenueRate)
                    .returns(Ratio.fromPercentage("4.99"), InvestmentLoanData::getInterestRate);
                softly.assertThat(investment.getLoan()
                    .getLabel())
                    .isEmpty();
                softly.assertThat(investment.getLoan()
                    .getDetailLabels())
                    .isEmpty();
                softly.assertThat(investment.getLoan()
                    .getStory())
                    .isEmpty();
                softly.assertThat(investment.getLoan()
                    .getTitle())
                    .isNotBlank();
                // Health stats are missing.
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .isEmpty();
            });
        }
    }

    @Test
    void deserializePending() throws Exception {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            Investment investment = jsonb.fromJson(PENDING_INVESTMENT_JSON, InvestmentImpl.class);
            assertSoftly(softly -> {
                softly.assertThat(investment.getId())
                    .isEqualTo(14300760);
                softly.assertThat(investment.getSellStatus())
                    .isEqualTo(SellStatus.NOT_SELLABLE);
                softly.assertThat(investment.getPrincipal())
                    .isEqualTo(new AmountsImpl(Money.from(200)));
                softly.assertThat(investment.getInterest())
                    .isEqualTo(new AmountsImpl(Money.from(26.93)));
                // SellInfo is missing.
                softly.assertThat(investment.getSmpSellInfo())
                    .isEmpty();
                // Loan data is present and contains particular values
                softly.assertThat(investment.getLoan())
                    .returns(769283, InvestmentLoanData::getId)
                    .returns(0, InvestmentLoanData::getDpd)
                    .returns(false, InvestmentLoanData::hasCollectionHistory)
                    .returns(Optional.of(Money.from(1925)), InvestmentLoanData::getAnnuity)
                    .returns(new InstalmentsImpl(60), InvestmentLoanData::getPayments)
                    .returns(Purpose.HOUSEHOLD, InvestmentLoanData::getPurpose)
                    .returns(Optional.of(MainIncomeType.EMPLOYMENT), l -> l.getBorrower()
                        .getPrimaryIncomeType())
                    .returns(Region.JIHOMORAVSKY, l -> l.getBorrower()
                        .getRegion())
                    .returns(Ratio.fromPercentage("4.49"), InvestmentLoanData::getRevenueRate)
                    .returns(Ratio.fromPercentage("4.99"), InvestmentLoanData::getInterestRate);
                softly.assertThat(investment.getLoan()
                    .getLabel())
                    .contains(PENDING);
                softly.assertThat(investment.getLoan()
                    .getDetailLabels())
                    .containsOnly(CURRENTLY_INSURED, VERIFIED_BORROWER, VERIFIED_INCOME);
                softly.assertThat(investment.getLoan()
                    .getStory())
                    .isNotEmpty();
                softly.assertThat(investment.getLoan()
                    .getTitle())
                    .isNotBlank();
                // Health stats are present and contain particular values
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getInstalmentsCurrentlyInDue)
                    .contains(OptionalInt.empty());
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getLoanHealthInfo)
                    .contains(LoanHealth.HEALTHY);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getInstalmentsCurrentlyInDue)
                    .contains(OptionalInt.empty());
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getDaysSinceLastInDue)
                    .contains(0);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getDueInstalments)
                    .contains(0);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getLongestDaysDue)
                    .contains(1);
                softly.assertThat(investment.getLoan()
                    .getHealthStats())
                    .map(LoanHealthStats::getPaidInstalments)
                    .contains(0);
            });
        }
    }

}
