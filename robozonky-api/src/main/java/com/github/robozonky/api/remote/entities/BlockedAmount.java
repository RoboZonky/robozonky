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
import java.util.Objects;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.enums.TransactionCategory;
import com.github.robozonky.internal.util.DateUtil;
import com.github.robozonky.internal.util.RandomUtil;

public class BlockedAmount extends BaseEntity {

    private BigDecimal amount, discount;
    private int id, loanId;
    private TransactionCategory category;
    private String loanName;
    private OffsetDateTime dateStart;

    public BlockedAmount(final BigDecimal loanAmount) {
        this(0, loanAmount);
    }

    public BlockedAmount(final int loanId, final BigDecimal loanAmount) {
        this(loanId, loanAmount, TransactionCategory.INVESTMENT);
    }

    public BlockedAmount(final int loanId, final BigDecimal loanAmount,
                         final TransactionCategory category) {
        this.id = RandomUtil.getNextInt(1_000_000_000);
        this.loanId = loanId;
        this.amount = loanAmount;
        this.category = category;
        this.dateStart = DateUtil.offsetNow();
    }

    private BlockedAmount() {
        // for JAXB
    }

    @XmlElement
    public int getId() {
        return id;
    }

    @XmlElement
    public BigDecimal getAmount() {
        return amount;
    }

    @XmlElement
    public BigDecimal getDiscount() {
        return discount;
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

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final BlockedAmount that = (BlockedAmount) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
