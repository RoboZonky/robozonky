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
import java.util.Comparator;
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;

import com.github.triceo.robozonky.api.remote.enums.TransactionCategory;

public class BlockedAmount extends BaseEntity implements Comparable<BlockedAmount> {

    private BigDecimal amount;
    private int loanId;
    private TransactionCategory category;
    private String loanName;
    private OffsetDateTime dateStart;

    public BlockedAmount(final BigDecimal loanAmount) {
        this(0, loanAmount, TransactionCategory.INVESTMENT_FEE);
    }

    public BlockedAmount(final int loanId, final BigDecimal loanAmount) {
        this(loanId, loanAmount, TransactionCategory.INVESTMENT);
    }

    public BlockedAmount(final int loanId, final BigDecimal loanAmount, final TransactionCategory category) {
        this.loanId = loanId;
        this.amount = loanAmount;
        this.category = category;
        this.dateStart = OffsetDateTime.now();
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

    /**
     * There is no way how a blocked amount with the same loan, same amount and the same category could be a different
     * blocked amount. We do not include start date, as that is artificial when the blocked amount instance is created
     * during local investing operations.
     * @param o
     * @return
     */
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final BlockedAmount that = (BlockedAmount) o;
        return loanId == that.loanId &&
                Objects.equals(amount, that.amount) &&
                category == that.category;
    }

    @Override
    public int hashCode() {
        return Objects.hash(amount, loanId, category);
    }

    @Override
    public int compareTo(final BlockedAmount blockedAmount) {
        return Comparator.comparing(BlockedAmount::getDateStart)
                .thenComparing(BlockedAmount::getLoanId)
                .thenComparing(BlockedAmount::getAmount)
                .compare(this, blockedAmount);
    }
}
