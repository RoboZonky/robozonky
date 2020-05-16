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

import java.net.URL;
import java.time.OffsetDateTime;
import java.util.StringJoiner;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.LoanHealthStats;
import com.github.robozonky.api.remote.entities.ParticipationDetail;
import com.github.robozonky.api.remote.enums.MainIncomeIndustry;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public class ParticipationDetailImpl implements ParticipationDetail {

    protected long id;
    protected long investmentId;
    protected MainIncomeType incomeType;
    protected MainIncomeIndustry mainIncomeIndustry;
    protected Ratio interestRate;
    protected Ratio revenueRate;
    protected URL url;
    protected Region region;
    protected LoanHealthStats loanHealthStats;
    protected String nextPaymentDate;
    protected Money amount;
    protected Money annuity;
    protected String name;
    protected Purpose purpose;
    protected Rating rating;
    protected int activeLoansCount;
    protected int dueInstalmentsCount;
    protected boolean insuranceActive;
    protected String story;

    public ParticipationDetailImpl() {
        // For JSON-B.
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public MainIncomeType getIncomeType() {
        return incomeType;
    }

    @Override
    public MainIncomeIndustry getMainIncomeIndustry() {
        return mainIncomeIndustry;
    }

    @Override
    public Ratio getInterestRate() {
        return interestRate;
    }

    @Override
    public Ratio getRevenueRate() {
        return revenueRate;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Purpose getPurpose() {
        return purpose;
    }

    @Override
    public URL getUrl() {
        return url;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public LoanHealthStats getLoanHealthStats() {
        return loanHealthStats;
    }

    @Override
    public OffsetDateTime getNextPaymentDate() {
        return OffsetDateTime.parse(nextPaymentDate);
    }

    @Override
    public Money getAmount() {
        return amount;
    }

    @Override
    public Money getAnnuity() {
        return annuity;
    }

    @Override
    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    @Override
    public int getDueInstalmentsCount() {
        return dueInstalmentsCount;
    }

    @Override
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @Override
    public String getStory() {
        return story;
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

    public void setMainIncomeIndustry(final MainIncomeIndustry mainIncomeIndustry) {
        this.mainIncomeIndustry = mainIncomeIndustry;
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    public void setRevenueRate(final Ratio revenueRate) {
        this.revenueRate = revenueRate;
    }

    public void setUrl(final URL url) {
        this.url = url;
    }

    public void setRegion(final Region region) {
        this.region = region;
    }

    public void setLoanHealthStats(final LoanHealthStats loanHealthStats) {
        this.loanHealthStats = loanHealthStats;
    }

    public void setNextPaymentDate(final OffsetDateTime nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate.toString();
    }

    public void setAmount(final Money amount) {
        this.amount = amount;
    }

    public void setAnnuity(final Money annuity) {
        this.annuity = annuity;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setPurpose(final Purpose purpose) {
        this.purpose = purpose;
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }

    public void setActiveLoansCount(final int activeLoansCount) {
        this.activeLoansCount = activeLoansCount;
    }

    public void setDueInstalmentsCount(final int dueInstalmentsCount) {
        this.dueInstalmentsCount = dueInstalmentsCount;
    }

    public void setInsuranceActive(final boolean insuranceActive) {
        this.insuranceActive = insuranceActive;
    }

    public void setStory(final String story) {
        this.story = story;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ParticipationDetailImpl.class.getSimpleName() + "[", "]")
            .add("activeLoansCount=" + activeLoansCount)
            .add("amount=" + amount)
            .add("annuity=" + annuity)
            .add("dueInstalmentsCount=" + dueInstalmentsCount)
            .add("id=" + id)
            .add("incomeType=" + incomeType)
            .add("insuranceActive=" + insuranceActive)
            .add("interestRate=" + interestRate)
            .add("investmentId=" + investmentId)
            .add("loanHealthStats=" + loanHealthStats)
            .add("mainIncomeIndustry=" + mainIncomeIndustry)
            .add("name='" + name + "'")
            .add("nextPaymentDate='" + nextPaymentDate + "'")
            .add("purpose=" + purpose)
            .add("rating=" + rating)
            .add("region=" + region)
            .add("revenueRate=" + revenueRate)
            .add("url=" + url)
            .toString();
    }
}
