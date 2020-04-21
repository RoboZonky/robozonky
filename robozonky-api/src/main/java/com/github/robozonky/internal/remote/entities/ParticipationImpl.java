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

import javax.json.bind.annotation.JsonbTypeAdapter;

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
import com.github.robozonky.internal.remote.adapters.CurrencyAdapter;

public class ParticipationImpl implements Participation {

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

    // Dates and times are expensive to parse, and Participations are on the hot path. Only do it when needed.
    protected String deadline;
    protected String nextPaymentDate;

    protected Country countryOfOrigin = Defaults.COUNTRY_OF_ORIGIN;
    @JsonbTypeAdapter(CurrencyAdapter.class)
    protected Currency currency;
    protected Money remainingPrincipal;
    protected Money discount;
    protected Money price;

    public ParticipationImpl() {
        // For JSON-B.
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
    public long getId() {
        return id;
    }

    @Override
    public long getInvestmentId() {
        return investmentId;
    }

    @Override
    public int getLoanId() {
        return loanId;
    }

    @Override
    public long getBorrowerNo() {
        return borrowerNo;
    }

    @Override
    public int getOriginalInstalmentCount() {
        return originalInstalmentCount;
    }

    @Override
    public int getRemainingInstalmentCount() {
        return remainingInstalmentCount;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public MainIncomeType getIncomeType() {
        return incomeType;
    }

    @Override
    public Ratio getInterestRate() {
        return interestRate;
    }

    @Override
    public String getLoanName() {
        return loanName;
    }

    @Override
    public Purpose getPurpose() {
        return purpose;
    }

    @Override
    public Rating getRating() {
        return rating;
    }

    @Override
    public boolean isWillExceedLoanInvestmentLimit() {
        return willExceedLoanInvestmentLimit;
    }

    @Override
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @Override
    public boolean isAdditionallyInsured() {
        return additionallyInsured;
    }

    @Override
    public LoanHealth getLoanHealthInfo() {
        return loanHealthInfo;
    }

    @Override
    public Country getCountryOfOrigin() {
        return countryOfOrigin;
    }

    @Override
    public Currency getCurrency() {
        return currency;
    }

    @Override
    public OffsetDateTime getDeadline() {
        return OffsetDateTime.parse(deadline);
    }

    @Override
    public LocalDate getNextPaymentDate() {
        return LocalDate.parse(nextPaymentDate);
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

    public void setBorrowerNo(final long borrowerNo) {
        this.borrowerNo = borrowerNo;
    }

    public void setLoanId(final int loanId) {
        this.loanId = loanId;
    }

    public void setOriginalInstalmentCount(final int originalInstalmentCount) {
        this.originalInstalmentCount = originalInstalmentCount;
    }

    public void setRemainingInstalmentCount(final int remainingInstalmentCount) {
        this.remainingInstalmentCount = remainingInstalmentCount;
    }

    public void setUserId(final int userId) {
        this.userId = userId;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public void setInvestmentId(final long investmentId) {
        this.investmentId = investmentId;
    }

    public void setIncomeType(final MainIncomeType incomeType) {
        this.incomeType = incomeType;
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    public void setLoanHealthInfo(final LoanHealth loanHealthInfo) {
        this.loanHealthInfo = loanHealthInfo;
    }

    public void setLoanName(final String loanName) {
        this.loanName = loanName;
    }

    public void setPurpose(final Purpose purpose) {
        this.purpose = purpose;
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }

    public void setWillExceedLoanInvestmentLimit(final boolean willExceedLoanInvestmentLimit) {
        this.willExceedLoanInvestmentLimit = willExceedLoanInvestmentLimit;
    }

    public void setInsuranceActive(final boolean insuranceActive) {
        this.insuranceActive = insuranceActive;
    }

    public void setAdditionallyInsured(final boolean additionallyInsured) {
        this.additionallyInsured = additionallyInsured;
    }

    public void setDeadline(final OffsetDateTime deadline) {
        this.deadline = deadline.toString();
    }

    public void setNextPaymentDate(final LocalDate nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate.toString();
    }

    public void setCountryOfOrigin(final Country countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public void setRemainingPrincipal(final Money remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public void setDiscount(final Money discount) {
        this.discount = discount;
    }

    public void setPrice(final Money price) {
        this.price = price;
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
