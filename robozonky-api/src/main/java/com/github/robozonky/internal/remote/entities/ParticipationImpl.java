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
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbTypeAdapter;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.Country;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeIndustry;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.util.json.CurrencyAdapter;

public class ParticipationImpl implements Participation {

    protected int loanId;
    protected int originalInstalmentCount;
    protected int remainingInstalmentCount;
    protected long id;
    protected long investmentId;
    protected MainIncomeType incomeType;
    protected MainIncomeIndustry mainIncomeIndustry;
    protected Ratio interestRate;
    protected LoanHealth loanHealthInfo;
    protected String loanName;
    protected Purpose purpose;
    protected Rating rating;
    protected boolean willExceedLoanInvestmentLimit;
    protected boolean insuranceActive;

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
        this.countryOfOrigin = loan.getCountryOfOrigin();
        this.currency = loan.getCurrency();
        this.incomeType = loan.getMainIncomeType();
        this.interestRate = loan.getInterestRate();
        this.loanName = loan.getName();
        this.originalInstalmentCount = loan.getTermInMonths();
        this.purpose = loan.getPurpose();
        this.rating = loan.getRating();
        this.insuranceActive = loan.isInsuranceActive();
        this.remainingPrincipal = remainingPrincipal;
        this.remainingInstalmentCount = remainingInstalmentCount;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public long getInvestmentId() {
        return investmentId;
    }

    public void setInvestmentId(final long investmentId) {
        this.investmentId = investmentId;
    }

    @Override
    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(final int loanId) {
        this.loanId = loanId;
    }

    @Override
    public int getOriginalInstalmentCount() {
        return originalInstalmentCount;
    }

    public void setOriginalInstalmentCount(final int originalInstalmentCount) {
        this.originalInstalmentCount = originalInstalmentCount;
    }

    @Override
    public int getRemainingInstalmentCount() {
        return remainingInstalmentCount;
    }

    public void setRemainingInstalmentCount(final int remainingInstalmentCount) {
        this.remainingInstalmentCount = remainingInstalmentCount;
    }

    @Override
    public MainIncomeType getIncomeType() {
        return incomeType;
    }

    public void setIncomeType(final MainIncomeType incomeType) {
        this.incomeType = incomeType;
    }

    @Override
    public MainIncomeIndustry getMainIncomeIndustry() {
        return mainIncomeIndustry;
    }

    @Override
    public Ratio getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    @Override
    public String getLoanName() {
        return loanName;
    }

    public void setLoanName(final String loanName) {
        this.loanName = loanName;
    }

    @Override
    public Purpose getPurpose() {
        return purpose;
    }

    public void setPurpose(final Purpose purpose) {
        this.purpose = purpose;
    }

    @Override
    public Rating getRating() {
        return rating;
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }

    @Override
    public boolean isWillExceedLoanInvestmentLimit() {
        return willExceedLoanInvestmentLimit;
    }

    public void setWillExceedLoanInvestmentLimit(final boolean willExceedLoanInvestmentLimit) {
        this.willExceedLoanInvestmentLimit = willExceedLoanInvestmentLimit;
    }

    @Override
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    public void setInsuranceActive(final boolean insuranceActive) {
        this.insuranceActive = insuranceActive;
    }

    @Override
    public LoanHealth getLoanHealthInfo() {
        return loanHealthInfo;
    }

    public void setLoanHealthInfo(final LoanHealth loanHealthInfo) {
        this.loanHealthInfo = loanHealthInfo;
    }

    @Override
    public Country getCountryOfOrigin() {
        return countryOfOrigin;
    }

    public void setCountryOfOrigin(final Country countryOfOrigin) {
        this.countryOfOrigin = countryOfOrigin;
    }

    @Override
    public Currency getCurrency() {
        return currency;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    @Override
    public Money getRemainingPrincipal() {
        return remainingPrincipal;
    }

    public void setRemainingPrincipal(final Money remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    @Override
    public Money getDiscount() {
        return discount;
    }

    public void setDiscount(final Money discount) {
        this.discount = discount;
    }

    @Override
    public Money getPrice() {
        return price;
    }

    public void setPrice(final Money price) {
        this.price = price;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ParticipationImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("loanId=" + loanId)
            .add("countryOfOrigin=" + countryOfOrigin)
            .add("currency=" + currency)
            .add("discount='" + discount + "'")
            .add("incomeType=" + incomeType)
            .add("mainIncomeIndustry=" + mainIncomeIndustry)
            .add("insuranceActive=" + insuranceActive)
            .add("interestRate=" + interestRate)
            .add("investmentId=" + investmentId)
            .add("loanHealthInfo=" + loanHealthInfo)
            .add("loanName='" + loanName + "'")
            .add("originalInstalmentCount=" + originalInstalmentCount)
            .add("price='" + price + "'")
            .add("purpose=" + purpose)
            .add("rating=" + rating)
            .add("remainingInstalmentCount=" + remainingInstalmentCount)
            .add("remainingPrincipal='" + remainingPrincipal + "'")
            .add("willExceedLoanInvestmentLimit=" + willExceedLoanInvestmentLimit)
            .toString();
    }
}
