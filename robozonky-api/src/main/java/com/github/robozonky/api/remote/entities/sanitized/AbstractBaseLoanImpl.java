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

package com.github.robozonky.api.remote.entities.sanitized;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Optional;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.BaseLoan;
import com.github.robozonky.api.remote.entities.InsurancePolicyPeriod;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.internal.test.RandomUtil;
import com.github.robozonky.internal.util.ToStringBuilder;
import io.vavr.Lazy;

@SuppressWarnings("unchecked")
abstract class AbstractBaseLoanImpl<T extends MutableBaseLoan<T>> implements MutableBaseLoan<T> {

    private final Lazy<String> toString = Lazy.of(() -> ToStringBuilder.createFor(this, "toString"));
    private boolean topped, covered, published, questionsAllowed, insuranceActive;
    private int id, termInMonths, investmentsCount, questionsCount, userId, activeLoansCount, amount;
    private int remainingInvestment;
    private int nonReservedRemainingInvestment;
    private String name, nickName, story;
    private Ratio interestRate;
    private Ratio investmentRate;
    private Ratio revenueRate;
    private BigDecimal annuity;
    private OffsetDateTime datePublished, deadline;
    private Rating rating;
    private MainIncomeType mainIncomeType;
    private Region region;
    private Purpose purpose;
    private Collection<InsurancePolicyPeriod> insuranceHistory = Collections.emptyList();
    private Currency currency;

    AbstractBaseLoanImpl() {
        this.id = RandomUtil.getNextInt(Integer.MAX_VALUE); // simplifies tests which do not need to set IDs themselves
    }

    AbstractBaseLoanImpl(final BaseLoan original) {
        this.currency = original.getCurrency();
        this.activeLoansCount = original.getActiveLoansCount();
        this.amount = (int) original.getAmount();
        this.datePublished = original.getDatePublished();
        this.deadline = original.getDeadline();
        this.id = original.getId();
        this.covered = original.isCovered();
        this.interestRate = original.getInterestRate();
        this.investmentsCount = original.getInvestmentsCount();
        this.investmentRate = original.getInvestmentRate();
        this.revenueRate = original.getRevenueRate();
        this.annuity = original.getAnnuityWithInsurance();
        this.insuranceActive = original.isInsuranceActive();
        this.published = original.isPublished();
        this.questionsAllowed = original.isQuestionsAllowed();
        this.topped = original.isTopped();
        this.mainIncomeType = original.getMainIncomeType();
        this.name = original.getName();
        this.nickName = original.getNickName();
        this.purpose = original.getPurpose();
        this.questionsCount = original.getQuestionsCount();
        this.rating = original.getRating();
        this.region = original.getRegion();
        this.remainingInvestment = (int) original.getRemainingInvestment();
        this.nonReservedRemainingInvestment = (int) (original.getRemainingInvestment() - original.getReservedAmount());
        this.story = original.getStory();
        this.termInMonths = original.getTermInMonths();
        this.userId = original.getUserId();
        setInsuranceHistory(original.getInsuranceHistory());
    }

    @Override
    public T setCurrency(final Currency currency) {
        this.currency = currency;
        return (T) this;
    }

    @Override
    public Currency getCurrency() {
        return currency;
    }


    @Override
    public MainIncomeType getMainIncomeType() {
        return mainIncomeType;
    }

    @Override
    public T setMainIncomeType(final MainIncomeType mainIncomeType) {
        this.mainIncomeType = mainIncomeType;
        return (T) this;
    }

    @Override
    public Ratio getInvestmentRate() {
        return investmentRate;
    }

    @Override
    public T setInvestmentRate(final Ratio investmentRate) {
        this.investmentRate = investmentRate;
        return (T) this;
    }

    @Override
    public Region getRegion() {
        return region;
    }

    @Override
    public T setRegion(final Region region) {
        this.region = region;
        return (T) this;
    }

    @Override
    public Purpose getPurpose() {
        return purpose;
    }

    @Override
    public T setPurpose(final Purpose purpose) {
        this.purpose = purpose;
        return (T) this;
    }

    @Override
    public int getId() {
        return id;
    }

    @Override
    public T setId(final int id) {
        this.id = id;
        return (T) this;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public T setName(final String name) {
        this.name = name;
        return (T) this;
    }

    @Override
    public String getStory() {
        return story;
    }

    @Override
    public T setStory(final String story) {
        this.story = story;
        return (T) this;
    }

    @Override
    public String getNickName() {
        return nickName;
    }

    @Override
    public T setNickName(final String nickName) {
        this.nickName = nickName;
        return (T) this;
    }

    @Override
    public int getTermInMonths() {
        return termInMonths;
    }

    @Override
    public T setTermInMonths(final int termInMonths) {
        this.termInMonths = termInMonths;
        return (T) this;
    }

    @Override
    public Ratio getInterestRate() {
        return interestRate;
    }

    @Override
    public T setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
        return (T) this;
    }

    @Override
    public Rating getRating() {
        return rating;
    }

    @Override
    public T setRating(final Rating rating) {
        this.rating = rating;
        return (T) this;
    }

    @Override
    public boolean isTopped() {
        return topped;
    }

    @Override
    public int getAmount() {
        return amount;
    }

    @Override
    public T setAmount(final int amount) {
        this.amount = amount;
        return (T) this;
    }

    @Override
    public int getRemainingInvestment() {
        return remainingInvestment;
    }

    @Override
    public T setRemainingInvestment(final int remainingInvestment) {
        this.remainingInvestment = remainingInvestment;
        return (T) this;
    }

    @Override
    public boolean isCovered() {
        return covered;
    }

    @Override
    public boolean isPublished() {
        return published;
    }

    @Override
    public OffsetDateTime getDatePublished() {
        return datePublished;
    }

    @Override
    public T setDatePublished(final OffsetDateTime datePublished) {
        this.datePublished = datePublished;
        return (T) this;
    }

    @Override
    public OffsetDateTime getDeadline() {
        return deadline;
    }

    @Override
    public T setDeadline(final OffsetDateTime deadline) {
        this.deadline = deadline;
        return (T) this;
    }

    @Override
    public int getInvestmentsCount() {
        return investmentsCount;
    }

    @Override
    public T setInvestmentsCount(final int investmentsCount) {
        this.investmentsCount = investmentsCount;
        return (T) this;
    }

    @Override
    public int getActiveLoansCount() {
        return activeLoansCount;
    }

    @Override
    public T setActiveLoansCount(final int activeLoansCount) {
        this.activeLoansCount = activeLoansCount;
        return (T) this;
    }

    @Override
    public int getQuestionsCount() {
        return questionsCount;
    }

    @Override
    public T setQuestionsCount(final int questionsCount) {
        this.questionsCount = questionsCount;
        return (T) this;
    }

    @Override
    public boolean isQuestionsAllowed() {
        return questionsAllowed;
    }

    @Override
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @Override
    public int getUserId() {
        return userId;
    }

    @Override
    public T setUserId(final int userId) {
        this.userId = userId;
        return (T) this;
    }

    @Override
    public T setTopped(final boolean isTopped) {
        this.topped = isTopped;
        return (T) this;
    }

    @Override
    public T setCovered(final boolean isCovered) {
        this.covered = isCovered;
        return (T) this;
    }

    @Override
    public T setPublished(final boolean isPublished) {
        this.published = isPublished;
        return (T) this;
    }

    @Override
    public T setQuestionsAllowed(final boolean isQuestionsAllowed) {
        this.questionsAllowed = isQuestionsAllowed;
        return (T) this;
    }

    @Override
    public T setInsuranceActive(final boolean isInsuranceActive) {
        this.insuranceActive = isInsuranceActive;
        return (T) this;
    }

    @Override
    public T setInsuranceHistory(final Collection<InsurancePolicyPeriod> insuranceHistory) {
        final boolean isEmpty = insuranceHistory == null || insuranceHistory.isEmpty();
        this.insuranceHistory = isEmpty ? Collections.emptyList() : new ArrayList<>(insuranceHistory);
        return (T) this;
    }

    @Override
    public Collection<InsurancePolicyPeriod> getInsuranceHistory() {
        return Collections.unmodifiableCollection(insuranceHistory);
    }

    @Override
    public int getNonReservedRemainingInvestment() {
        return nonReservedRemainingInvestment;
    }

    @Override
    public T setNonReservedRemainingInvestment(final int remainingInvestment) {
        this.nonReservedRemainingInvestment = remainingInvestment;
        return (T) this;
    }

    @Override
    public T setRevenueRate(final Ratio revenueRate) {
        this.revenueRate = revenueRate;
        return (T) this;
    }

    @Override
    public T setAnnuity(final BigDecimal annuity) {
        this.annuity = annuity;
        return (T) this;
    }

    @Override
    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    @Override
    public BigDecimal getAnnuity() {
        return annuity;
    }

    @Override
    public final String toString() {
        return toString.get();
    }
}
