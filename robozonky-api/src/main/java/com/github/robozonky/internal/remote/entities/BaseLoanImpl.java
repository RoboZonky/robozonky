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
import java.util.Optional;
import java.util.StringJoiner;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.BaseLoan;
import com.github.robozonky.api.remote.enums.MainIncomeIndustry;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public abstract class BaseLoanImpl implements BaseLoan {

    protected boolean insuranceActive;
    protected int id;
    protected int termInMonths;
    protected String name;
    protected String story;
    protected Rating rating;
    protected MainIncomeType mainIncomeType;
    protected MainIncomeIndustry mainIncomeIndustry;
    protected Region region;
    protected Purpose purpose;

    // various ratios
    protected Ratio interestRate;
    protected Ratio revenueRate;

    // OffsetDateTime is expensive to parse, and Loans are on the hot path. Only do it when needed.
    protected String datePublished;

    protected Money amount;
    protected Money remainingInvestment;
    protected Money reservedAmount;
    protected Money annuity;
    protected Money annuityWithInsurance;
    protected Money premium;
    protected Money zonkyPlusAmount;

    @Override
    public MainIncomeType getMainIncomeType() {
        return mainIncomeType;
    }

    public void setMainIncomeType(final MainIncomeType mainIncomeType) {
        this.mainIncomeType = mainIncomeType;
    }

    @Override
    public MainIncomeIndustry getMainIncomeIndustry() {
        return mainIncomeIndustry;
    }

    public void setMainIncomeIndustry(final MainIncomeIndustry mainIncomeIndustry) {
        this.mainIncomeIndustry = mainIncomeIndustry;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    public void setRegion(final Region region) {
        this.region = region;
    }

    @Override
    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(final Purpose purpose) {
        this.purpose = purpose;
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Override
    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getStory() {
        return story;
    }

    public void setStory(final String story) {
        this.story = story;
    }

    @Override
    public int getTermInMonths() {
        return termInMonths;
    }

    public void setTermInMonths(final int termInMonths) {
        this.termInMonths = termInMonths;
    }

    @Override
    public Ratio getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    @Override
    public Rating getRating() {
        return rating;
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }

    @Override
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    public void setInsuranceActive(final boolean insuranceActive) {
        this.insuranceActive = insuranceActive;
    }

    @Override
    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    public void setRevenueRate(final Ratio revenueRate) {
        this.revenueRate = revenueRate;
    }

    @Override
    public OffsetDateTime getDatePublished() {
        return OffsetDateTime.parse(datePublished);
    }

    public void setDatePublished(final OffsetDateTime datePublished) {
        this.datePublished = datePublished.toString();
    }

    @Override
    public Money getAmount() {
        return amount;
    }

    public void setAmount(final Money amount) {
        this.amount = amount;
    }

    @Override
    public Money getRemainingInvestment() {
        return remainingInvestment;
    }

    public void setRemainingInvestment(final Money remainingInvestment) {
        this.remainingInvestment = remainingInvestment;
    }

    @Override
    public Money getNonReservedRemainingInvestment() {
        return getRemainingInvestment().subtract(reservedAmount);
    }

    @Override
    public Money getReservedAmount() {
        return reservedAmount;
    }

    public void setReservedAmount(final Money reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    @Override
    public Money getZonkyPlusAmount() {
        return zonkyPlusAmount;
    }

    public void setZonkyPlusAmount(final Money zonkyPlusAmount) {
        this.zonkyPlusAmount = zonkyPlusAmount;
    }

    @Override
    public Money getAnnuity() {
        return annuity;
    }

    public void setAnnuity(final Money annuity) {
        this.annuity = annuity;
    }

    @Override
    public Money getPremium() {
        return premium;
    }

    public void setPremium(final Money premium) {
        this.premium = premium;
    }

    @Override
    public Money getAnnuityWithInsurance() {
        return annuityWithInsurance;
    }

    public void setAnnuityWithInsurance(final Money annuityWithInsurance) {
        this.annuityWithInsurance = annuityWithInsurance;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BaseLoanImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("amount='" + amount + "'")
            .add("annuity='" + annuity + "'")
            .add("annuityWithInsurance='" + annuityWithInsurance + "'")
            .add("datePublished='" + datePublished + "'")
            .add("insuranceActive=" + insuranceActive)
            .add("interestRate=" + interestRate)
            .add("mainIncomeType=" + mainIncomeType)
            .add("mainIncomeIndustry=" + mainIncomeIndustry)
            .add("name='" + name + "'")
            .add("premium='" + premium + "'")
            .add("purpose=" + purpose)
            .add("rating=" + rating)
            .add("region=" + region)
            .add("remainingInvestment='" + remainingInvestment + "'")
            .add("reservedAmount='" + reservedAmount + "'")
            .add("revenueRate=" + revenueRate)
            .add("termInMonths=" + termInMonths)
            .add("zonkyPlusAmount='" + zonkyPlusAmount + "'")
            .toString();
    }
}
