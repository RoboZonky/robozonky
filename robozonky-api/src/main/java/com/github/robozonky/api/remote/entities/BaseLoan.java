/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.internal.Defaults;
import io.vavr.Lazy;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Optional;

public abstract class BaseLoan extends BaseEntity {

    private boolean topped;
    private boolean covered;
    private boolean published;
    private boolean questionsAllowed;
    private boolean insuranceActive;
    private boolean insuredInFuture;
    private boolean additionallyInsured;
    private boolean multicash;
    private int id;
    private int termInMonths;
    private int investmentsCount;
    private int questionsCount;
    private int userId;
    private int activeLoansCount;
    private String name;
    private String nickName;
    private String story;
    private OffsetDateTime datePublished;
    private OffsetDateTime deadline;
    private Rating rating;
    private Collection<Photo> photos;
    private BorrowerRelatedInvestmentInfo borrowerRelatedInvestmentInfo;
    private OtherInvestments myOtherInvestments;
    private MainIncomeType mainIncomeType;
    private Region region;
    private Purpose purpose;
    @XmlElement
    private Collection<InsurancePolicyPeriod> insuranceHistory;

    // various ratios

    private Ratio interestRate;
    private Ratio investmentRate;
    @XmlElement
    private Ratio revenueRate;

    private Currency currency = Defaults.CURRENCY;

    // strings to be represented as money

    @XmlElement
    private String amount;
    private final Lazy<Money> moneyAmount = Lazy.of(() -> Money.from(amount, currency));
    @XmlElement
    private String remainingInvestment;
    private final Lazy<Money> moneyRemainingInvestment = Lazy.of(() -> Money.from(remainingInvestment, currency));
    @XmlElement
    private String reservedAmount;
    private final Lazy<Money> moneyReservedAmount = Lazy.of(() -> Money.from(reservedAmount, currency));
    @XmlElement
    private String annuity;
    private final Lazy<Money> moneyAnnuity = Lazy.of(() -> Money.from(annuity, currency));
    @XmlElement
    private String annuityWithInsurance;
    private final Lazy<Money> moneyAnnuityWithInsurance = Lazy.of(() -> Money.from(annuityWithInsurance, currency));
    @XmlElement
    private String premium;
    private final Lazy<Money> moneyPremium = Lazy.of(() -> Money.from(premium, currency));
    @XmlElement
    private String zonkyPlusAmount;
    private final Lazy<Money> moneyZonkyPlusAmount = Lazy.of(() -> Money.from(zonkyPlusAmount, currency));

    protected BaseLoan() {
        // for JAXB
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
    public boolean isMulticash() {
        return multicash;
    }

    @XmlElement
    public boolean isPublished() {
        return published;
    }

    @XmlElement
    public OffsetDateTime getDatePublished() {
        return datePublished;
    }

    @XmlElement
    public OffsetDateTime getDeadline() {
        return deadline;
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

    @XmlElement
    public Collection<Photo> getPhotos() {
        return photos;
    }

    /**
     * Holds the same information as {@link #getBorrowerRelatedInvestmentInfo()}, no need to use this.
     * @return
     */
    @XmlElement
    public OtherInvestments getMyOtherInvestments() {
        return myOtherInvestments;
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

    /**
     * @return
     */
    @XmlElement
    public Collection<InsurancePolicyPeriod> getInsuranceHistory() {
        return insuranceHistory == null ? Collections.emptySet() : insuranceHistory;
    }

    /**
     * @return Null unless the loan was queried using {@link LoanApi#item(int)}.
     */
    @XmlElement
    public BorrowerRelatedInvestmentInfo getBorrowerRelatedInvestmentInfo() {
        return borrowerRelatedInvestmentInfo;
    }

    @XmlElement
    public int getUserId() {
        return userId;
    }

    @XmlTransient
    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    // money-based fields are all transient

    @XmlTransient
    public Money getAmount() {
        return moneyAmount.get();
    }

    @XmlTransient
    public Money getRemainingInvestment() {
        return moneyRemainingInvestment.get();
    }

    @XmlTransient
    public Money getNonReservedRemainingInvestment() {
        return moneyRemainingInvestment.get().subtract(moneyReservedAmount.get());
    }

    @XmlTransient
    public Money getReservedAmount() {
        return moneyReservedAmount.get();
    }

    @XmlTransient
    public Money getZonkyPlusAmount() {
        return moneyZonkyPlusAmount.get();
    }

    @XmlTransient
    public Money getAnnuity() {
        return moneyAnnuity.get();
    }

    @XmlTransient
    public Money getPremium() {
        return moneyPremium.get();
    }

    /**
     * @return {@link #getAnnuity()} + {@link #getPremium()}
     */
    @XmlTransient
    public Money getAnnuityWithInsurance() {
        return moneyAnnuityWithInsurance.get();
    }
}
