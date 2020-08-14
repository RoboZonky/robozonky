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

import static java.util.Objects.requireNonNull;

import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Borrower;
import com.github.robozonky.api.remote.entities.Instalments;
import com.github.robozonky.api.remote.entities.InvestmentLoanData;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.LoanHealthStats;
import com.github.robozonky.api.remote.enums.DetailLabel;
import com.github.robozonky.api.remote.enums.Label;
import com.github.robozonky.api.remote.enums.Purpose;

public class InvestmentLoanDataImpl implements InvestmentLoanData {

    private int id;
    private int activeLoanOrdinal;
    private String title;
    private String story;
    private Money annuity;
    @JsonbProperty(nillable = true)
    private Label label;
    @JsonbProperty(nillable = true)
    private Set<DetailLabel> detailLabels;
    private Borrower borrower;
    private LoanHealthStats healthStats;
    private Purpose purpose;
    private Instalments payments;
    private Ratio revenueRate;
    private Ratio interestRate;

    public InvestmentLoanDataImpl() {
        // For JSON-B.
    }

    public InvestmentLoanDataImpl(Loan loan) {
        this(loan, null);
    }

    public InvestmentLoanDataImpl(Loan loan, LoanHealthStats loanHealthStats) {
        this.id = loan.getId();
        this.title = loan.getName();
        this.story = loan.getStory();
        this.annuity = loan.getAnnuity();
        this.borrower = new BorrowerImpl(loan.getMainIncomeType(), loan.getRegion());
        this.healthStats = loanHealthStats;
        this.purpose = loan.getPurpose();
        this.payments = new InstalmentsImpl(loan.getTermInMonths());
        this.revenueRate = loan.getRevenueRate()
            .orElse(loan.getRating()
                .getMaximalRevenueRate());
        this.interestRate = loan.getInterestRate();
    }

    @Override
    public int getId() {
        return id;
    }

    public void setId(final int id) {
        this.id = id;
    }

    @Override
    public int getActiveLoanOrdinal() {
        return activeLoanOrdinal;
    }

    public void setActiveLoanOrdinal(final int activeLoanOrdinal) {
        this.activeLoanOrdinal = activeLoanOrdinal;
    }

    @Override
    public String getTitle() {
        return requireNonNull(title);
    }

    public void setTitle(final String title) {
        this.title = title;
    }

    @Override
    public String getStory() {
        return requireNonNull(story);
    }

    public void setStory(final String story) {
        this.story = story;
    }

    @Override
    public Money getAnnuity() {
        return requireNonNull(annuity);
    }

    public void setAnnuity(final Money annuity) {
        this.annuity = annuity;
    }

    @Override
    public Optional<Label> getLabel() {
        return Optional.ofNullable(label);
    }

    public void setLabel(final Label label) {
        this.label = label;
    }

    @Override
    public Set<DetailLabel> getDetailLabels() {
        return Optional.ofNullable(detailLabels)
            .map(Collections::unmodifiableSet)
            .orElse(Collections.emptySet());
    }

    public void setDetailLabels(final Set<DetailLabel> detailLabels) {
        this.detailLabels = EnumSet.copyOf(detailLabels);
    }

    @Override
    public Borrower getBorrower() {
        return requireNonNull(borrower);
    }

    public void setBorrower(final Borrower borrower) {
        this.borrower = borrower;
    }

    @Override
    public LoanHealthStats getHealthStats() {
        return requireNonNull(healthStats);
    }

    public void setHealthStats(final LoanHealthStats healthStats) {
        this.healthStats = healthStats;
    }

    @Override
    public Purpose getPurpose() {
        return requireNonNull(purpose);
    }

    public void setPurpose(final Purpose purpose) {
        this.purpose = purpose;
    }

    @Override
    public Instalments getPayments() {
        return requireNonNull(payments);
    }

    public void setPayments(final Instalments payments) {
        this.payments = payments;
    }

    @Override
    public Ratio getRevenueRate() {
        return requireNonNull(revenueRate);
    }

    public void setRevenueRate(final Ratio revenueRate) {
        this.revenueRate = revenueRate;
    }

    @Override
    public Ratio getInterestRate() {
        return requireNonNull(interestRate);
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InvestmentLoanDataImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("annuity=" + annuity)
            .add("interestRate=" + interestRate)
            .add("revenueRate=" + revenueRate)
            .add("purpose=" + purpose)
            .add("payments=" + payments)
            .add("healthStats=" + healthStats)
            .add("borrower=" + borrower)
            .add("title='" + title + "'")
            .add("label=" + label)
            .add("detailLabels=" + detailLabels)
            .add("activeLoanOrdinal=" + activeLoanOrdinal)
            .toString();
    }
}
