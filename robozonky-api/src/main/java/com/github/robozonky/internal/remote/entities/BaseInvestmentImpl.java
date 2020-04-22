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

import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.Optional;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbTypeAdapter;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.BaseInvestment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;
import com.github.robozonky.internal.util.json.CurrencyAdapter;

/**
 * Do not use instances of this class directly. Instead, use {@link InvestmentImpl}. Otherwise you may be bitten by
 * various quirks of the Zonky API, returning null in unexpected places.
 */
public abstract class BaseInvestmentImpl implements BaseInvestment {

    private long id;
    private int loanId;
    @JsonbTypeAdapter(CurrencyAdapter.class)
    private Currency currency = Defaults.CURRENCY;
    private Money amount;
    private InvestmentStatus status;
    private OffsetDateTime timeCreated = DateUtil.offsetNow();

    public BaseInvestmentImpl() {
        // For JSON-B.
    }

    BaseInvestmentImpl(final Loan loan, final Money amount) {
        this.currency = amount.getCurrency();
        this.loanId = loan.getId();
        this.amount = amount;
        this.status = InvestmentStatus.ACTIVE;
    }

    @Override
    public Optional<OffsetDateTime> getTimeCreated() {
        return Optional.ofNullable(timeCreated);
    }

    @Override
    public InvestmentStatus getStatus() {
        return status;
    }

    @Override
    public int getLoanId() {
        return loanId;
    }

    @Override
    public Currency getCurrency() {
        return currency;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public Money getAmount() {
        return amount;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public void setLoanId(final int loanId) {
        this.loanId = loanId;
    }

    public void setCurrency(final Currency currency) {
        this.currency = currency;
    }

    public void setAmount(final Money amount) {
        this.amount = amount;
    }

    public void setStatus(final InvestmentStatus status) {
        this.status = status;
    }

    public void setTimeCreated(final OffsetDateTime timeCreated) {
        this.timeCreated = timeCreated;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BaseInvestmentImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("loanId=" + loanId)
            .add("amount='" + amount + "'")
            .add("currency=" + currency)
            .add("status=" + status)
            .add("timeCreated=" + timeCreated)
            .toString();
    }
}
