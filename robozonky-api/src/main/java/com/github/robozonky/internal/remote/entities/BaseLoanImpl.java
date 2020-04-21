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
import java.util.Optional;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbTypeAdapter;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.BaseLoan;
import com.github.robozonky.api.remote.enums.Country;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.adapters.CurrencyAdapter;

public abstract class BaseLoanImpl implements BaseLoan {

    protected boolean topped;
    protected boolean covered;
    protected boolean published;
    protected boolean insuranceActive;
    protected boolean insuredInFuture;
    protected boolean additionallyInsured;
    protected int id;
    protected int termInMonths;
    protected int investmentsCount;
    protected int userId;
    protected int activeLoansCount;
    protected long publicIdentifier = 0;
    protected long borrowerNo = 0;
    protected String name;
    protected String nickName;
    protected String story;
    protected Rating rating;
    protected MainIncomeType mainIncomeType;
    protected Region region;
    protected Purpose purpose;

    // various ratios
    protected Ratio interestRate;
    protected Ratio investmentRate;
    protected Ratio revenueRate;

    // OffsetDateTime is expensive to parse, and Loans are on the hot path. Only do it when needed.
    protected String datePublished;
    protected String deadline;

    protected Country countryOfOrigin = Defaults.COUNTRY_OF_ORIGIN;
    @JsonbTypeAdapter(CurrencyAdapter.class)
    protected Currency currency = Defaults.CURRENCY;

    protected Money amount;
    protected Money remainingInvestment;
    protected Money reservedAmount;
    protected Money annuity;
    protected Money annuityWithInsurance;
    protected Money premium;
    protected Money zonkyPlusAmount;

    @Override
    public Country getCountryOfOrigin() {
        return countryOfOrigin;
    }

    @Override
    public Currency getCurrency() {
        return currency;
    }

    @Override
    public MainIncomeType getMainIncomeType() {
        return mainIncomeType;
    }

    @Override
    public Ratio getInvestmentRate() {
        return investmentRate;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public Purpose getPurpose() {
        return purpose;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public long getPublicIdentifier() {
        return publicIdentifier;
    }

    @Override
    public long getBorrowerNo() {
        return borrowerNo;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getStory() {
        return story;
    }

    @Override
    public String getNickName() {
        return nickName;
    }

    @Override
    public int getTermInMonths() {
        return termInMonths;
    }

    @Override
    public Ratio getInterestRate() {
        return interestRate;
    }

    @Override
    public Rating getRating() {
        return rating;
    }

    @Override
    public boolean isTopped() {
        return topped;
    }

    @Override
    public boolean isCovered() {
        return covered;
    }

    @Override
    public boolean isPublished() {
        return published;
    }

    @Override
    public int getInvestmentsCount() {
        return investmentsCount;
    }

    @Override
    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    @Override
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @Override
    public boolean isInsuredInFuture() {
        return insuredInFuture;
    }

    @Override
    public boolean isAdditionallyInsured() {
        return additionallyInsured;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    @Override
    public OffsetDateTime getDatePublished() {
        return OffsetDateTime.parse(datePublished);
    }

    @Override
    public OffsetDateTime getDeadline() {
        return OffsetDateTime.parse(deadline);
    }

    @Override
    public Money getAmount() {
        return amount;
    }

    @Override
    public Money getRemainingInvestment() {
        return remainingInvestment;
    }

    @Override
    public Money getNonReservedRemainingInvestment() {
        return getRemainingInvestment().subtract(reservedAmount);
    }

    @Override
    public Money getReservedAmount() {
        return reservedAmount;
    }

    @Override
    public Money getZonkyPlusAmount() {
        return zonkyPlusAmount;
    }

    @Override
    public Money getAnnuity() {
        return annuity;
    }

    @Override
    public Money getPremium() {
        return premium;
    }

    @Override
    public Money getAnnuityWithInsurance() {
        return annuityWithInsurance;
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

    public void setPublicIdentifier(final long publicIdentifier) {
        this.publicIdentifier = publicIdentifier;
    }

    public void setBorrowerNo(final long borrowerNo) {
        this.borrowerNo = borrowerNo;
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

    public void setDatePublished(final OffsetDateTime datePublished) {
        this.datePublished = datePublished.toString();
    }

    public void setDeadline(final OffsetDateTime deadline) {
        this.deadline = deadline.toString();
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

    @Override
    public String toString() {
        return new StringJoiner(", ", BaseLoanImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("activeLoansCount=" + activeLoansCount)
            .add("additionallyInsured=" + additionallyInsured)
            .add("amount='" + amount + "'")
            .add("annuity='" + annuity + "'")
            .add("annuityWithInsurance='" + annuityWithInsurance + "'")
            .add("borrowerNo=" + borrowerNo)
            .add("countryOfOrigin=" + countryOfOrigin)
            .add("covered=" + covered)
            .add("currency=" + currency)
            .add("datePublished='" + datePublished + "'")
            .add("deadline='" + deadline + "'")
            .add("insuranceActive=" + insuranceActive)
            .add("insuredInFuture=" + insuredInFuture)
            .add("interestRate=" + interestRate)
            .add("investmentRate=" + investmentRate)
            .add("investmentsCount=" + investmentsCount)
            .add("mainIncomeType=" + mainIncomeType)
            .add("name='" + name + "'")
            .add("nickName='" + nickName + "'")
            .add("premium='" + premium + "'")
            .add("publicIdentifier=" + publicIdentifier)
            .add("published=" + published)
            .add("purpose=" + purpose)
            .add("rating=" + rating)
            .add("region=" + region)
            .add("remainingInvestment='" + remainingInvestment + "'")
            .add("reservedAmount='" + reservedAmount + "'")
            .add("revenueRate=" + revenueRate)
            .add("termInMonths=" + termInMonths)
            .add("topped=" + topped)
            .add("userId=" + userId)
            .add("zonkyPlusAmount='" + zonkyPlusAmount + "'")
            .toString();
    }
}
