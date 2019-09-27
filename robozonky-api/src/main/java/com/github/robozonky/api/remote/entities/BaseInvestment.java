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

package com.github.robozonky.api.remote.entities;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;
import io.vavr.Lazy;

import javax.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Currency;

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
    @XmlElement
    private String additionalAmount;
    @XmlElement
    private String firstAmount;
    private final Lazy<Money> moneyAmount = Lazy.of(() -> Money.from(amount, currency));
    private final Lazy<Money> moneyAdditionalAmount = Lazy.of(() -> Money.from(additionalAmount, currency));
    private final Lazy<Money> moneyFirstAmount = Lazy.of(() -> Money.from(firstAmount, currency));
    private InvestmentStatus status;
    private OffsetDateTime timeCreated = DateUtil.offsetNow();

    BaseInvestment() {
        // for JAXB
    }

    BaseInvestment(final Loan loan, final Money amount) {
        this.currency = amount.getCurrency();
        this.loanId = loan.getId();
        this.amount = amount.getValue().toPlainString();
        this.additionalAmount = BigDecimal.ZERO.toPlainString();
        this.firstAmount = amount.getValue().toPlainString();
        this.status = InvestmentStatus.ACTIVE;
    }

    @XmlElement
    public OffsetDateTime getTimeCreated() {
        return timeCreated;
    }

    @XmlElement
    public InvestmentStatus getStatus() {
        return status;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    public Money getAmount() {
        return moneyAmount.get();
    }

    public Money getAdditionalAmount() {
        return moneyAdditionalAmount.get();
    }

    public Money getFirstAmount() {
        return moneyFirstAmount.get();
    }

    @XmlElement
    public Currency getCurrency() {
        return currency;
    }

    @XmlElement
    public long getId() {
        return id;
    }
}
