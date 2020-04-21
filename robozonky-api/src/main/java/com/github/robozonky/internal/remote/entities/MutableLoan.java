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

import java.time.OffsetDateTime;
import java.util.Currency;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Country;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public final class MutableLoan extends LoanImpl {

    public void setUrl(final String url) {
        this.url = url;
    }

    public void setMyInvestment(final MyInvestmentImpl myInvestment) {
        this.myInvestment = myInvestment;
    }

    public void setTopped(final boolean topped) {
        this.topped = topped;
    }

    public void setCovered(final boolean covered) {
        this.covered = covered;
    }

    public void setPublished(final boolean published) {
        this.published = published;
    }

    public void setInsuranceActive(final boolean insuranceActive) {
        this.insuranceActive = insuranceActive;
    }

    public void setInsuredInFuture(final boolean insuredInFuture) {
        this.insuredInFuture = insuredInFuture;
    }

    public void setAdditionallyInsured(final boolean additionallyInsured) {
        this.additionallyInsured = additionallyInsured;
    }

    public void setId(final int id) {
        this.id = id;
    }

    public void setTermInMonths(final int termInMonths) {
        this.termInMonths = termInMonths;
    }

    public void setInvestmentsCount(final int investmentsCount) {
        this.investmentsCount = investmentsCount;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public void setActiveLoansCount(final int activeLoansCount) {
        this.activeLoansCount = activeLoansCount;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setNickName(final String nickName) {
        this.nickName = nickName;
    }

    public void setStory(final String story) {
        this.story = story;
    }

    public void setDatePublished(final OffsetDateTime datePublished) {
        this.datePublished = datePublished.toString();
    }

    public void setDeadline(final OffsetDateTime deadline) {
        this.deadline = deadline.toString();
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }

    public void setMainIncomeType(final MainIncomeType mainIncomeType) {
        this.mainIncomeType = mainIncomeType;
    }

    public void setRegion(final Region region) {
        this.region = region;
    }

    public void setPurpose(final Purpose purpose) {
        this.purpose = purpose;
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    public void setInvestmentRate(final Ratio investmentRate) {
        this.investmentRate = investmentRate;
    }

    public void setRevenueRate(final Ratio revenueRate) {
        this.revenueRate = revenueRate;
    }

    public void setCountryOfOrigin(final Country countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public void setAmount(final Money amount) {
        this.amount = amount;
    }

    public void setRemainingInvestment(final Money remainingInvestment) {
        this.remainingInvestment = remainingInvestment;
    }

    public void setReservedAmount(final Money reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public void setAnnuity(final Money annuity) {
        this.annuity = annuity;
    }

    public void setAnnuityWithInsurance(final Money annuityWithInsurance) {
        this.annuityWithInsurance = annuityWithInsurance;
    }

    public void setPremium(final Money premium) {
        this.premium = premium;
    }

    public void setZonkyPlusAmount(final Money zonkyPlusAmount) {
        this.zonkyPlusAmount = zonkyPlusAmount;
    }

}
