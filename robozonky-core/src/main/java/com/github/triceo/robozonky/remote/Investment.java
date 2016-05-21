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

public class Investment {

    private int loanId, amount;
    private Rating rating;

    Investment() {
        // just for JAXB
    }

    public Investment(final Loan loan, final int amount) {
        this.loanId = loan.getId();
        this.rating = loan.getRating();
        this.amount = amount;
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public int getAmount() {
        return amount;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Investment{");
        sb.append("loanId=").append(loanId);
        sb.append(", rating=").append(rating);
        sb.append(", amount=").append(amount);
        sb.append('}');
        return sb.toString();
    }
}
