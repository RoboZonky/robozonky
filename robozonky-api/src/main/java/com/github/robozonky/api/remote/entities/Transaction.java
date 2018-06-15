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

package com.github.robozonky.api.remote.entities;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.api.remote.enums.TransactionOrientation;

public class Transaction extends BaseEntity {

    private BigDecimal amount, discount;
    private TransactionCategory category;
    private TransactionOrientation orientation;
    private OffsetDateTime transactionDate;
    private String customMessage;
    private int id, loanId, investmentId;
    private String loanName;
    private String nickName;

    public Transaction(final Loan loan, final BigDecimal amount, final TransactionCategory category,
                       final TransactionOrientation orientation) {
        this(0, loan, amount, category, orientation);
    }

    public Transaction(final int id, final Loan loan, final BigDecimal amount, final TransactionCategory category,
                       final TransactionOrientation orientation) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.orientation = orientation;
        this.transactionDate = OffsetDateTime.now();
        this.customMessage = "";
        this.loanId = loan.getId();
        loan.getMyInvestment().ifPresent(i -> this.investmentId = i.getId());
        this.loanName = loan.getName();
        this.nickName = loan.getNickName();
        this.discount = BigDecimal.ZERO;
    }

    public Transaction(final Investment investment, final BigDecimal amount, final TransactionCategory category,
                       final TransactionOrientation orientation) {
        this(0, investment, amount, category, orientation);
    }

    public Transaction(final int id, final Investment investment, final BigDecimal amount,
                       final TransactionCategory category, final TransactionOrientation orientation) {
        this.id = id;
        this.amount = amount;
        this.category = category;
        this.orientation = orientation;
        this.transactionDate = OffsetDateTime.now();
        this.customMessage = "";
        this.loanId = investment.getLoanId();
        this.investmentId = investment.getId();
        this.loanName = "";
        this.nickName = "";
        this.discount = BigDecimal.ZERO;
    }

    private Transaction() {
        // for JAXB
    }

    @XmlElement
    public BigDecimal getAmount() {
        return amount;
    }

    @XmlElement
    public TransactionCategory getCategory() {
        return category;
    }

    @XmlElement
    public TransactionOrientation getOrientation() {
        return orientation;
    }

    @XmlElement
    public OffsetDateTime getTransactionDate() {
        return transactionDate;
    }

    @XmlElement
    public String getCustomMessage() {
        return customMessage;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public int getInvestmentId() {
        return investmentId;
    }

    @XmlElement
    public String getLoanName() {
        return loanName;
    }

    @XmlElement
    public String getNickName() {
        return nickName;
    }

    @XmlElement
    public BigDecimal getDiscount() {
        return discount;
    }

    @XmlElement
    public int getId() {
        return id;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final Transaction that = (Transaction) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
