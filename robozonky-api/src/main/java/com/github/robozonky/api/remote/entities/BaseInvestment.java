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

package com.github.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import java.util.Currency;
import java.util.Optional;
import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;

/**
 * Do not use instances of this class directly. Instead, use {@link Investment}. Otherwise you may be bitten by
 * various quirks of the Zonky API, returning null in unexpected places.
 */
abstract class BaseInvestment extends BaseEntity {

    private long id;
    private int loanId;
    private Currency currency = Defaults.CURRENCY;
    @XmlElement
    private String amount;
    private InvestmentStatus status;
    @XmlElement
    private OffsetDateTime timeCreated = DateUtil.offsetNow();

    BaseInvestment() {
        // for JAXB
    }

    BaseInvestment(final Loan loan, final Money amount) {
        this.currency = amount.getCurrency();
        this.loanId = loan.getId();
        this.amount = amount.getValue()
            .toPlainString();
        this.status = InvestmentStatus.ACTIVE;
    }

    @XmlTransient
    public Optional<OffsetDateTime> getTimeCreated() {
        return Optional.ofNullable(timeCreated);
    }

    @XmlElement
    public InvestmentStatus getStatus() {
        return status;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public Currency getCurrency() {
        return currency;
    }

    @XmlElement
    public long getId() {
        return id;
    }

    // Money types are all transient.

    @XmlTransient
    public Money getAmount() {
        return Money.from(amount, currency);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", BaseInvestment.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("loanId=" + loanId)
            .add("amount='" + amount + "'")
            .add("currency=" + currency)
            .add("status=" + status)
            .add("timeCreated=" + timeCreated)
            .toString();
    }
}
