/*
 * Copyright 2017 Lukáš Petrovický
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
import com.github.triceo.robozonky.api.remote.enums.TransactionOrientation;

public class Transaction extends BaseEntity {

    private BigDecimal amount;
    private TransactionCategory category;
    private TransactionOrientation orientation;
    private OffsetDateTime transactionDate;
    private String customMessage;
    private int loanId;
    private String loanName;
    private String nickName;

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
    public String getLoanName() {
        return loanName;
    }

    @XmlElement
    public String getNickName() {
        return nickName;
    }
}
