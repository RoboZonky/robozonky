/*
 * Copyright 2018 The RoboZonky Project
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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

/**
 * This class carries several enumeration-based fields. Some of the enums are extremely important to the core function
 * (such as {@link Rating}), while others ({@link Region}, {@link MainIncomeType}, {@link Purpose}) are only providing
 * additional metadata. If the important enums change, we need RoboZonky to fail. However, in case of the others, we
 * provide non-failing deserializers which handle the missing values gracefully and provide a message warning users that
 * something needs an upgrade.
 * <p>
 * It is not recommended to use this class directly as Zonky will return various null references for fields at various
 * points in the investment lifecycle. Please use {@link Loan} and {@link MarketplaceLoan} as a null-safe alternative.
 * Instances may be created with static methods such as {@link Loan#sanitized(RawLoan)}.
 */
public class RawLoan extends BaseEntity {

    private boolean topped, covered, published, questionsAllowed, insuranceActive, multicash, fastcash;
    private int id, termInMonths, investmentsCount, questionsCount, userId, activeLoansCount;
    private double amount, remainingInvestment;
    private String name, nickName, story, url;
    private BigDecimal interestRate;
    private OffsetDateTime datePublished, deadline;
    private Rating rating;
    private Collection<Photo> photos;
    private BigDecimal investmentRate;
    private BorrowerRelatedInvestmentInfo borrowerRelatedInvestmentInfo;
    private MyInvestment myInvestment;
    private OtherInvestments myOtherInvestments;
    private MainIncomeType mainIncomeType;
    private Region region;
    private Purpose purpose;
    private Collection<InsurancePolicyPeriod> insuranceHistory;

    protected RawLoan() {
        // for JAXB
    }

    /**
     * @return Null if the loan doesn't have an investment by the current user.
     */
    @XmlElement
    public MyInvestment getMyInvestment() {
        return myInvestment;
    }

    @XmlElement
    public MainIncomeType getMainIncomeType() {
        return mainIncomeType;
    }

    @XmlElement
    public BigDecimal getInvestmentRate() {
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
    public BigDecimal getInterestRate() {
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
    public double getAmount() {
        return amount;
    }

    @XmlElement
    public double getRemainingInvestment() {
        return remainingInvestment;
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
    public boolean isFastcash() {
        return fastcash;
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

    @XmlElement
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @XmlElement
    public Collection<InsurancePolicyPeriod> getInsuranceHistory() {
        return insuranceHistory;
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

    @XmlElement
    public String getUrl() {
        return url;
    }
}
