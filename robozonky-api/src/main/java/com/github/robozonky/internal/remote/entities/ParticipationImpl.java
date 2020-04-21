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

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.Country;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.Defaults;

public class ParticipationImpl extends BaseEntity implements Participation {

    protected long borrowerNo;
    protected int loanId;
    protected int originalInstalmentCount;
    protected int remainingInstalmentCount;
    protected int userId;
    protected long id;
    protected long investmentId;
    protected MainIncomeType incomeType;
    protected Ratio interestRate;
    protected LoanHealth loanHealthInfo;
    protected String loanName;
    protected Purpose purpose;
    protected Rating rating;
    protected boolean willExceedLoanInvestmentLimit;
    protected boolean insuranceActive;
    protected boolean additionallyInsured;
    private Object loanInvestments;

    // Dates and times are expensive to parse, and Participations are on the hot path. Only do it when needed.
    @XmlElement
    protected String deadline;
    @XmlElement
    protected String nextPaymentDate;

    protected Country countryOfOrigin = Defaults.COUNTRY_OF_ORIGIN;
    protected Currency currency;
    @XmlElement
    protected Money remainingPrincipal;
    @XmlElement
    protected Money discount;
    @XmlElement
    protected Money price;

    ParticipationImpl() {
        // for JAXB
    }

    public ParticipationImpl(final Loan loan, final Money remainingPrincipal, final int remainingInstalmentCount) {
        this.loanId = loan.getId();
        this.borrowerNo = loan.getBorrowerNo();
        this.countryOfOrigin = loan.getCountryOfOrigin();
        this.currency = loan.getCurrency();
        this.incomeType = loan.getMainIncomeType();
        this.interestRate = loan.getInterestRate();
        this.loanName = loan.getName();
        this.originalInstalmentCount = loan.getTermInMonths();
        this.purpose = loan.getPurpose();
        this.rating = loan.getRating();
        this.userId = loan.getUserId();
        this.additionallyInsured = loan.isAdditionallyInsured();
        this.insuranceActive = loan.isInsuranceActive();
        this.remainingPrincipal = remainingPrincipal;
        this.remainingInstalmentCount = remainingInstalmentCount;
    }

    @Override
    @XmlElement
    public long getId() {
        return id;
    }

    @Override
    @XmlElement
    public long getInvestmentId() {
        return investmentId;
    }

    @Override
    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @Override
    @XmlElement
    public long getBorrowerNo() {
        return borrowerNo;
    }

    @Override
    @XmlElement
    public int getOriginalInstalmentCount() {
        return originalInstalmentCount;
    }

    @Override
    @XmlElement
    public int getRemainingInstalmentCount() {
        return remainingInstalmentCount;
    }

    @Override
    @XmlElement
    public int getUserId() {
        return userId;
    }

    @Override
    @XmlElement
    public MainIncomeType getIncomeType() {
        return incomeType;
    }

    @Override
    @XmlElement
    public Ratio getInterestRate() {
        return interestRate;
    }

    @Override
    @XmlElement
    public String getLoanName() {
        return loanName;
    }

    @Override
    @XmlElement
    public Purpose getPurpose() {
        return purpose;
    }

    @Override
    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @Override
    @XmlElement
    public boolean isWillExceedLoanInvestmentLimit() {
        return willExceedLoanInvestmentLimit;
    }

    @Override
    @XmlElement
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @Override
    @XmlElement
    public boolean isAdditionallyInsured() {
        return additionallyInsured;
    }

    @Override
    @XmlElement
    public Object getLoanInvestments() { // FIXME figure out what this means
        return loanInvestments;
    }

    @Override
    @XmlElement
    public LoanHealth getLoanHealthInfo() {
        return loanHealthInfo;
    }

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
    @XmlTransient
    public OffsetDateTime getDeadline() {
        return OffsetDateTimeAdapter.fromString(deadline);
    }

    @Override
    @XmlTransient
    public LocalDate getNextPaymentDate() {
        return LocalDateAdapter.fromString(nextPaymentDate);
    }

    @Override
    public Money getRemainingPrincipal() {
        return remainingPrincipal;
    }

    @Override
    public Money getDiscount() {
        return discount;
    }

    @Override
    public Money getPrice() {
        return price;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ParticipationImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("loanId=" + loanId)
            .add("additionallyInsured=" + additionallyInsured)
            .add("borrowerNo=" + borrowerNo)
            .add("countryOfOrigin=" + countryOfOrigin)
            .add("currency=" + currency)
            .add("deadline='" + deadline + "'")
            .add("discount='" + discount + "'")
            .add("incomeType=" + incomeType)
            .add("insuranceActive=" + insuranceActive)
            .add("interestRate=" + interestRate)
            .add("investmentId=" + investmentId)
            .add("loanHealthInfo=" + loanHealthInfo)
            .add("loanName='" + loanName + "'")
            .add("nextPaymentDate='" + nextPaymentDate + "'")
            .add("originalInstalmentCount=" + originalInstalmentCount)
            .add("price='" + price + "'")
            .add("purpose=" + purpose)
            .add("rating=" + rating)
            .add("remainingInstalmentCount=" + remainingInstalmentCount)
            .add("remainingPrincipal='" + remainingPrincipal + "'")
            .add("userId=" + userId)
            .add("willExceedLoanInvestmentLimit=" + willExceedLoanInvestmentLimit)
            .toString();
    }
}
