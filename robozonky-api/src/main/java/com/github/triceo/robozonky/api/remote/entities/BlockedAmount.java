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
import javax.xml.bind.annotation.XmlElement;

import com.github.triceo.robozonky.api.remote.enums.TransactionCategory;

public class BlockedAmount extends BaseEntity {

    private BigDecimal amount;
    private int loanId;
    private TransactionCategory category;
    private String loanName;
    private OffsetDateTime dateStart;

    public BlockedAmount(final BigDecimal loanAmount) {
        this.loanId = 0;
        this.amount = loanAmount;
        this.category = TransactionCategory.INVESTMENT_FEE;
    }

    public BlockedAmount(final int loanId, final BigDecimal loanAmount) {
        this.loanId = loanId;
        this.amount = loanAmount;
        this.category = TransactionCategory.INVESTMENT;
    }

    private BlockedAmount() {
        // for JAXB
    }

    @XmlElement
    public BigDecimal getAmount() {
        return amount;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public TransactionCategory getCategory() {
        return category;
    }

    @XmlElement
    public String getLoanName() {
        return loanName;
    }

    @XmlElement
    public OffsetDateTime getDateStart() {
        return dateStart;
    }
}
