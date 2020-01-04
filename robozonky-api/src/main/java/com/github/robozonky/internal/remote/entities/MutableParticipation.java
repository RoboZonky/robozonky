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

import java.util.Currency;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.Country;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;

public final class MutableParticipation extends Participation {

    public MutableParticipation(Loan loan, final Money remainingPrincipal, final int remainingInstalmentCount) {
        super(loan, remainingPrincipal, remainingInstalmentCount);
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

    public void setDeadline(final String deadline) {
        this.deadline = deadline;
    }

    public void setNextPaymentDate(final String nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
    }

    public void setCountryOfOrigin(final Country countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public void setRemainingPrincipal(final String remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public void setDiscount(final String discount) {
        this.discount = discount;
    }

    public void setPrice(final String price) {
        this.price = price;
    }

}
