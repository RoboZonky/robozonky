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

package com.github.robozonky.api.remote.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.enums.InvestmentStatus;

/**
 * Do not use instances of this class directly. Instead, use {@link Investment}. Otherwise you may be bitten by
 * various quirks of the Zonky API, returning null in unexpected places.
 */
abstract class BaseInvestment extends BaseEntity {

    private long id;
    private int loanId;
    private BigDecimal amount, additionalAmount, firstAmount;
    private InvestmentStatus status;
    private OffsetDateTime timeCreated = OffsetDateTime.MIN;

    BaseInvestment() {
        // for JAXB
    }

    BaseInvestment(final Investment investment) {
        this.id = investment.getId();
        this.loanId = investment.getLoanId();
        this.amount = investment.getOriginalPrincipal();
        this.additionalAmount = BigDecimal.ZERO;
        this.firstAmount = BigDecimal.ZERO;
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
    public long getId() {
        return id;
    }
}
