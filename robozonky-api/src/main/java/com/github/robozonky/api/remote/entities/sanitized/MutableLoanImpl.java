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

package com.github.robozonky.api.remote.entities.sanitized;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.BorrowerRelatedInvestmentInfo;
import com.github.robozonky.api.remote.entities.RawLoan;

final class MutableLoanImpl extends AbstractMutableLoanImpl<LoanBuilder> implements LoanBuilder {

    private BigDecimal remainingPrincipalToLoan, remainingPrincipalToBorrower, totalPrincipalToLoan,
            totalPrincipalToBorrower;
    private SortedSet<String> knownBorrowerNicknames;

    MutableLoanImpl() {

    }

    MutableLoanImpl(final RawLoan original) {
        super(original);
        final BorrowerRelatedInvestmentInfo i = original.getBorrowerRelatedInvestmentInfo();
        if (i == null) {
            this.setKnownBorrowerNicknames(Collections.emptyList());
        } else {
            this.remainingPrincipalToBorrower = i.getRemainingPrincipalToBorrower();
            this.remainingPrincipalToLoan = i.getRemainingPrincipalToLoan();
            this.totalPrincipalToBorrower = i.getTotalPrincipalToBorrower();
            this.totalPrincipalToLoan = i.getTotalPrincipalToLoan();
            this.setKnownBorrowerNicknames(i.getOtherBorrowerNicknames());
        }
    }

    @Override
    public LoanBuilder setRemainingPrincipalToLoan(final BigDecimal remainingPrincipalToLoan) {
        this.remainingPrincipalToLoan = remainingPrincipalToLoan;
        return this;
    }

    @Override
    public LoanBuilder setTotalPrincipalToLoan(final BigDecimal totalPrincipalToLoan) {
        this.totalPrincipalToLoan = totalPrincipalToLoan;
        return this;
    }

    @Override
    public LoanBuilder setRemainingPrincipalToBorrower(final BigDecimal remainingPrincipalToBorrower) {
        this.remainingPrincipalToBorrower = remainingPrincipalToBorrower;
        return this;
    }

    @Override
    public LoanBuilder setTotalPrincipalToBorrower(final BigDecimal totalPrincipalToBorrower) {
        this.totalPrincipalToBorrower = totalPrincipalToBorrower;
        return this;
    }

    @Override
    public LoanBuilder setKnownBorrowerNicknames(final Collection<String> knownBorrowerNicknames) {
        if (knownBorrowerNicknames == null) {
            this.knownBorrowerNicknames = Collections.emptySortedSet();
        } else {
            final SortedSet<String> result = Stream.concat(knownBorrowerNicknames.stream(), Stream.of(getNickName()))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(TreeSet::new));
            this.knownBorrowerNicknames = Collections.unmodifiableSortedSet(result);
        }
        return this;
    }

    @Override
    public Optional<BigDecimal> getRemainingPrincipalToLoan() {
        return Optional.ofNullable(remainingPrincipalToLoan);
    }

    @Override
    public Optional<BigDecimal> getTotalPrincipalToLoan() {
        return Optional.ofNullable(totalPrincipalToLoan);
    }

    @Override
    public Optional<BigDecimal> getRemainingPrincipalToBorrower() {
        return Optional.ofNullable(remainingPrincipalToBorrower);
    }

    @Override
    public Optional<BigDecimal> getTotalPrincipalToBorrower() {
        return Optional.ofNullable(totalPrincipalToBorrower);
    }

    @Override
    public Collection<String> getKnownBorrowerNicknames() {
        return knownBorrowerNicknames;
    }

}
