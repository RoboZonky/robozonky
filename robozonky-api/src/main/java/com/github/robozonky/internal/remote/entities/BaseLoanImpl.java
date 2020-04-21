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

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.BaseLoan;
import com.github.robozonky.api.remote.enums.Country;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.internal.Defaults;

public abstract class BaseLoanImpl extends BaseEntity implements BaseLoan {

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
    @XmlElement
    protected Ratio revenueRate;

    // OffsetDateTime is expensive to parse, and Loans are on the hot path. Only do it when needed.
    @XmlElement
    protected String datePublished;
    @XmlElement
    protected String deadline;

    protected Country countryOfOrigin = Defaults.COUNTRY_OF_ORIGIN;
    protected Currency currency = Defaults.CURRENCY;

    @XmlElement
    protected Money amount;
    @XmlElement
    protected Money remainingInvestment;
    @XmlElement
    protected Money reservedAmount;
    @XmlElement
    protected Money annuity;
    @XmlElement
    protected Money annuityWithInsurance;
    @XmlElement
    protected Money premium;
    @XmlElement
    protected Money zonkyPlusAmount;

    /*
     * Don't waste time deserializing some types, as we're never going to use them. Yet we do not want these reported as
     * unknown fields by Jackson.
     */
    @XmlElement
    private Object photos;
    @XmlElement
    private Object flags;
    @XmlElement
    private Object borrowerRelatedInvestmentInfo;
    @XmlElement
    private Object myOtherInvestments;
    @XmlElement
    private Object insuranceHistory;

    @Override
    @XmlElement
    public Country getCountryOfOrigin() {
        return countryOfOrigin;
    }

    @Override
    @XmlElement
    public Currency getCurrency() {
        return currency;
    }

    @Override
    @XmlElement
    public MainIncomeType getMainIncomeType() {
        return mainIncomeType;
    }

    @Override
    @XmlElement
    public Ratio getInvestmentRate() {
        return investmentRate;
    }

    @Override
    @XmlElement
    public Region getRegion() {
        return region;
    }

    @Override
    @XmlElement
    public Purpose getPurpose() {
        return purpose;
    }

    @Override
    @XmlElement
    public int getId() {
        return id;
    }

    @Override
    @XmlElement
    public long getPublicIdentifier() {
        return publicIdentifier;
    }

    @Override
    @XmlElement
    public long getBorrowerNo() {
        return borrowerNo;
    }

    @Override
    @XmlElement
    public String getName() {
        return name;
    }

    @Override
    @XmlElement
    public String getStory() {
        return story;
    }

    @Override
    @XmlElement
    public String getNickName() {
        return nickName;
    }

    @Override
    @XmlElement
    public int getTermInMonths() {
        return termInMonths;
    }

    @Override
    @XmlElement
    public Ratio getInterestRate() {
        return interestRate;
    }

    @Override
    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @Override
    @XmlElement
    public boolean isTopped() {
        return topped;
    }

    @Override
    @XmlElement
    public boolean isCovered() {
        return covered;
    }

    @Override
    @XmlElement
    public boolean isPublished() {
        return published;
    }

    @Override
    @XmlElement
    public int getInvestmentsCount() {
        return investmentsCount;
    }

    @Override
    @XmlElement
    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    @Override
    @XmlElement
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @Override
    @XmlElement
    public boolean isInsuredInFuture() {
        return insuredInFuture;
    }

    @Override
    @XmlElement
    public boolean isAdditionallyInsured() {
        return additionallyInsured;
    }

    @Override
    @XmlElement
    public int getUserId() {
        return userId;
    }

    @Override
    @XmlTransient
    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    // Datetime fields are all transient.

    @Override
    @XmlTransient
    public OffsetDateTime getDatePublished() {
        return OffsetDateTimeAdapter.fromString(datePublished);
    }

    @Override
    @XmlTransient
    public OffsetDateTime getDeadline() {
        return OffsetDateTimeAdapter.fromString(deadline);
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
