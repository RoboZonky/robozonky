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

import java.util.Optional;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Amounts;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.InvestmentLoanData;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.enums.SellStatus;

public class InvestmentImpl implements Investment {

    private long id;
    private InvestmentLoanData loan;
    @JsonbProperty(nillable = true)
    private SellInfo smpSellInfo;
    private Amounts principal;
    private Amounts interest;
    private SellStatus sellStatus;

    public InvestmentImpl() {
        // For JSON-B.
    }

    public InvestmentImpl(final InvestmentLoanData loan, final Money amount) {
        this.principal = new AmountsImpl(amount);
        this.loan = loan;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public InvestmentLoanData getLoan() {
        return requireNonNull(loan);
    }

    public void setLoan(final InvestmentLoanData loan) {
        this.loan = loan;
    }

    @Override
    public Optional<SellInfo> getSmpSellInfo() {
        return Optional.ofNullable(smpSellInfo);
    }

    public void setSmpSellInfo(final SellInfo smpSellInfo) {
        this.smpSellInfo = smpSellInfo;
    }

    @Override
    public Amounts getPrincipal() {
        return requireNonNull(principal);
    }

    public void setPrincipal(final Amounts principal) {
        this.principal = principal;
    }

    @Override
    public Amounts getInterest() {
        return requireNonNull(interest);
    }

    public void setInterest(final Amounts interest) {
        this.interest = interest;
    }

    @Override
    public SellStatus getSellStatus() {
        return requireNonNull(sellStatus);
    }

    public void setSellStatus(final SellStatus sellStatus) {
        this.sellStatus = sellStatus;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InvestmentImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("loan=" + loan)
            .add("principal=" + principal)
            .add("interest=" + interest)
            .add("sellStatus=" + sellStatus)
            .add("smpSellInfo=" + smpSellInfo)
            .toString();
    }
}
