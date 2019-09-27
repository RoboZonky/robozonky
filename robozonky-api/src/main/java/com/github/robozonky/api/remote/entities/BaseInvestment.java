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

import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;

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
    private BigDecimal amount;
    private BigDecimal additionalAmount;
    private BigDecimal firstAmount;
    private InvestmentStatus status;
    private OffsetDateTime timeCreated = DateUtil.offsetNow();

    BaseInvestment() {
        // for JAXB
    }

    BaseInvestment(final Loan loan, final BigDecimal amount, final Currency currency) {
        this.currency = currency;
        this.loanId = loan.getId();
        this.amount = amount;
        this.additionalAmount = BigDecimal.ZERO;
        this.firstAmount = amount;
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

    @XmlElement
    public BigDecimal getAmount() {
        return amount;
    }

    @XmlElement
    public BigDecimal getAdditionalAmount() {
        return additionalAmount;
    }

    @XmlElement
    public BigDecimal getFirstAmount() {
        return firstAmount;
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
