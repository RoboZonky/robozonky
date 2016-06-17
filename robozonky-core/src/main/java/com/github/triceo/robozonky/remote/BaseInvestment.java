/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.remote;

import javax.xml.bind.annotation.XmlElement;

public abstract class BaseInvestment implements BaseEntity {

    private int id, loanId, amount, additionalAmount, firstAmount;

    BaseInvestment() {
        // for JAXB
    }

    BaseInvestment(final Loan loan, final int amount) {
        this.loanId = loan.getId();
        this.amount = amount;
        if (loan.getMyInvestment() != null) {
            final MyInvestment m = loan.getMyInvestment();
            this.id = m.getId();
            this.additionalAmount = m.getAdditionalAmount();
            this.firstAmount = m.getFirstAmount();
        }
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
    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Investment{");
        sb.append("id=").append(id);
        sb.append(", loanId=").append(this.getLoanId());
        sb.append(", amount=").append(this.getAmount());
        sb.append('}');
        return sb.toString();
    }
}
