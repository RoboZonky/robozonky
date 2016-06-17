/*
 * Copyright 2016 Lukáš Petrovický
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
package com.github.triceo.robozonky.remote;

import java.time.Instant;
import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class MyInvestment implements BaseEntity {

    private int id, loanId, amount, additionalAmount, firstAmount, investorId;
    private String status, investorNickname;
    private Instant timeCreated;

    MyInvestment() {
        // for JAXB
    }

    @XmlElement
    public int getId() {
        return id;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public int getAmount() {
        return amount;
    }

    @XmlElement
    public int getAdditionalAmount() {
        return additionalAmount;
    }

    @XmlElement
    public int getFirstAmount() {
        return firstAmount;
    }

    @XmlElement
    public int getInvestorId() {
        return investorId;
    }

    @XmlElement
    public String getStatus() {
        return status;
    }

    @XmlElement
    public String getInvestorNickname() {
        return investorNickname;
    }

    @XmlElement
    @JsonDeserialize(using = InstantDeserializer.class)
    public Instant getTimeCreated() {
        return timeCreated;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("MyInvestment{");
        sb.append("id=").append(id);
        sb.append(", loanId=").append(loanId);
        sb.append(", amount=").append(amount);
        sb.append(", additionalAmount=").append(additionalAmount);
        sb.append(", firstAmount=").append(firstAmount);
        sb.append(", status='").append(status).append('\'');
        sb.append(", timeCreated=").append(timeCreated);
        sb.append('}');
        return sb.toString();
    }
}
