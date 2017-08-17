/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.triceo.robozonky.api.remote.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.triceo.robozonky.api.remote.enums.InvestmentStatus;

abstract class BaseInvestment extends BaseEntity {

    private int id, loanId;
    private BigDecimal amount, additionalAmount, firstAmount;
    private Loan loan;
    private InvestmentStatus status;
    private OffsetDateTime timeCreated;

    BaseInvestment() {
        // for JAXB
    }

    BaseInvestment(final Loan loan, final BigDecimal amount) {
        this.loan = loan;
        this.loanId = loan.getId();
        this.amount = amount;
        this.status = InvestmentStatus.ACTIVE;
        this.timeCreated = OffsetDateTime.MIN;
        if (loan.getMyInvestment() != null) {
            final MyInvestment m = loan.getMyInvestment();
            this.id = m.getId();
            this.additionalAmount = m.getAdditionalAmount();
            this.firstAmount = m.getFirstAmount();
        }
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
    public int getId() {
        return id;
    }

    @XmlTransient
    public Optional<Loan> getLoan() {
        return Optional.ofNullable(loan);
    }
}
