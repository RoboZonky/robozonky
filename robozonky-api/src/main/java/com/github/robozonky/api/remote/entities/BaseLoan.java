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

package com.github.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.Optional;
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Country;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.internal.Defaults;

public abstract class BaseLoan extends BaseEntity {

    protected boolean topped;
    protected boolean covered;
    protected boolean published;
    protected boolean questionsAllowed;
    protected boolean insuranceActive;
    protected boolean insuredInFuture;
    protected boolean additionallyInsured;
    protected int id;
    protected int termInMonths;
    protected int investmentsCount;
    protected int questionsCount;
    protected int userId;
    protected int activeLoansCount;
    protected long borrowerNo;
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

    // Strings to be represented as money.
    @XmlElement
    protected String amount;
    @XmlElement
    protected String remainingInvestment;
    @XmlElement
    protected String reservedAmount;
    @XmlElement
    protected String annuity;
    @XmlElement
    protected String annuityWithInsurance;
    @XmlElement
    protected String premium;
    @XmlElement
    protected String zonkyPlusAmount;

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

    @XmlElement
    public Country getCountryOfOrigin() {
        return countryOfOrigin;
    }

    @XmlElement
    public Currency getCurrency() {
        return currency;
    }

    @XmlElement
    public MainIncomeType getMainIncomeType() {
        return mainIncomeType;
    }

    @XmlElement
    public Ratio getInvestmentRate() {
        return investmentRate;
    }

    @XmlElement
    public Region getRegion() {
        return region;
    }

    @XmlElement
    public Purpose getPurpose() {
        return purpose;
    }

    @XmlElement
    public int getId() {
        return id;
    }

    @XmlElement
    public long getBorrowerNo() {
        return borrowerNo;
    }

    @XmlElement
    public String getName() {
        return name;
    }

    @XmlElement
    public String getStory() {
        return story;
    }

    @XmlElement
    public String getNickName() {
        return nickName;
    }

    @XmlElement
    public int getTermInMonths() {
        return termInMonths;
    }

    @XmlElement
    public Ratio getInterestRate() {
        return interestRate;
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @XmlElement
    public boolean isTopped() {
        return topped;
    }

    @XmlElement
    public boolean isCovered() {
        return covered;
    }

    @XmlElement
    public boolean isPublished() {
        return published;
    }

    @XmlElement
    public int getInvestmentsCount() {
        return investmentsCount;
    }

    @XmlElement
    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    @XmlElement
    public int getQuestionsCount() {
        return questionsCount;
    }

    @XmlElement
    public boolean isQuestionsAllowed() {
        return questionsAllowed;
    }

    /**
     * @return True if the loan is insured at this very moment. Uninsured loans will have both this and
     * {@link #isAdditionallyInsured()} return false.
     */
    @XmlElement
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    /**
     * @return True if the loan will become insured at some later point in time. False when {@link #isInsuranceActive()}
     * is true.
     */
    @XmlElement
    public boolean isInsuredInFuture() {
        return insuredInFuture;
    }

    /**
     * @return If the loan is insured, but did not start this way. Uninsured loans will have both this and
     * {@link #isInsuranceActive()} return false.
     */
    @XmlElement
    public boolean isAdditionallyInsured() {
        return additionallyInsured;
    }

    @XmlElement
    public int getUserId() {
        return userId;
    }

    @XmlTransient
    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    // Datetime fields are all transient.

    @XmlTransient
    public OffsetDateTime getDatePublished() {
        return OffsetDateTimeAdapter.fromString(datePublished);
    }

    @XmlTransient
    public OffsetDateTime getDeadline() {
        return OffsetDateTimeAdapter.fromString(deadline);
    }

    // Money-based fields are all transient.

    @XmlTransient
    public Money getAmount() {
        return Money.from(amount, currency);
    }

    @XmlTransient
    public Money getRemainingInvestment() {
        return Money.from(remainingInvestment, currency);
    }

    @XmlTransient
    public Money getNonReservedRemainingInvestment() {
        return getRemainingInvestment().subtract(Money.from(reservedAmount, currency));
    }

    @XmlTransient
    public Money getReservedAmount() {
        return Money.from(reservedAmount, currency);
    }

    @XmlTransient
    public Money getZonkyPlusAmount() {
        return Money.from(zonkyPlusAmount, currency);
    }

    @XmlTransient
    public Money getAnnuity() {
        return Money.from(annuity, currency);
    }

    @XmlTransient
    public Money getPremium() {
        return Money.from(premium, currency);
    }

    /**
     * @return {@link #getAnnuity()} + {@link #getPremium()}
     */
    @XmlTransient
    public Money getAnnuityWithInsurance() {
        return Money.from(annuityWithInsurance, currency);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BaseLoan.class.getSimpleName() + "[", "]")
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
                .add("published=" + published)
                .add("purpose=" + purpose)
                .add("questionsAllowed=" + questionsAllowed)
                .add("questionsCount=" + questionsCount)
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
