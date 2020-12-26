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

package com.github.robozonky.internal.remote;

import java.util.Optional;
import java.util.Set;
import java.util.StringJoiner;
import java.util.function.Supplier;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Borrower;
import com.github.robozonky.api.remote.entities.Instalments;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.InvestmentLoanData;
import com.github.robozonky.api.remote.entities.LoanHealthStats;
import com.github.robozonky.api.remote.enums.DetailLabel;
import com.github.robozonky.api.remote.enums.Label;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;

final class AutoExtendingInvestmentLoanDataImpl implements InvestmentLoanData {

    private final InvestmentLoanData delegate;
    private final Supplier<InvestmentLoanData> fullInvestmentLoanData;

    public AutoExtendingInvestmentLoanDataImpl(InvestmentLoanData investmentLoanData,
            Supplier<Investment> fullInvestmentSupplier) {
        this.delegate = investmentLoanData;
        this.fullInvestmentLoanData = () -> fullInvestmentSupplier.get()
            .getLoan();
    }

    @Override
    public int getId() {
        return delegate.getId();
    }

    @Override
    public int getActiveLoanOrdinal() {
        return delegate.getActiveLoanOrdinal();
    }

    @Override
    public int getDpd() {
        return delegate.getDpd();
    }

    @Override
    public boolean hasCollectionHistory() {
        return delegate.hasCollectionHistory();
    }

    @Override
    public Rating getRating() {
        return delegate.getRating();
    }

    @Override
    public String getTitle() {
        return delegate.getTitle();
    }

    @Override
    public Optional<String> getStory() {
        return delegate.getStory();
    }

    @Override
    public Optional<Money> getAnnuity() {
        return delegate.getAnnuity();
    }

    @Override
    public Optional<Label> getLabel() {
        return delegate.getLabel()
            .or(() -> fullInvestmentLoanData.get()
                .getLabel());
    }

    @Override
    public Set<DetailLabel> getDetailLabels() {
        return fullInvestmentLoanData.get()
            .getDetailLabels();
    }

    @Override
    public Borrower getBorrower() {
        // primaryIncomeType is missing in the original
        return fullInvestmentLoanData.get()
            .getBorrower();
    }

    @Override
    public Optional<LoanHealthStats> getHealthStats() {
        return delegate.getHealthStats()
            .or(() -> fullInvestmentLoanData.get()
                .getHealthStats());
    }

    @Override
    public Purpose getPurpose() {
        return delegate.getPurpose();
    }

    @Override
    public Instalments getPayments() {
        return delegate.getPayments();
    }

    @Override
    public Ratio getRevenueRate() {
        return delegate.getRevenueRate();
    }

    @Override
    public Ratio getInterestRate() {
        return delegate.getInterestRate();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AutoExtendingInvestmentLoanDataImpl.class.getSimpleName() + "[", "]")
            .add("delegate=" + delegate)
            .toString();
    }
}
