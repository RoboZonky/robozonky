/*
 * Copyright 2020 The RoboZonky Project
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

import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Rating;

public class RiskPortfolio extends BaseEntity {

    private Ratio interestRate;
    @XmlElement
    private String unpaid;
    @XmlElement
    private String paid;
    @XmlElement
    private String due;
    @XmlElement
    private String totalAmount;
    private Rating rating;

    RiskPortfolio() {
        // for JAXB
    }

    public RiskPortfolio(final Rating rating, final Money paid, final Money unpaid, final Money due) {
        this.interestRate = rating.getInterestRate();
        this.paid = paid.getValue()
            .toPlainString();
        this.unpaid = unpaid.getValue()
            .toPlainString();
        this.due = due.getValue()
            .toPlainString();
        this.rating = rating;
        this.totalAmount = paid.add(unpaid)
            .add(due)
            .getValue()
            .toPlainString();
    }

    @XmlElement
    public Ratio getInterestRate() {
        return interestRate;
    }

    @XmlTransient
    public Money getUnpaid() {
        return Money.from(unpaid);
    }

    @XmlTransient
    public Money getPaid() {
        return Money.from(paid);
    }

    @XmlTransient
    public Money getDue() {
        return Money.from(due);
    }

    @XmlTransient
    public Money getTotalAmount() {
        return Money.from(totalAmount);
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", RiskPortfolio.class.getSimpleName() + "[", "]")
            .add("due='" + due + "'")
            .add("interestRate=" + interestRate)
            .add("paid='" + paid + "'")
            .add("rating=" + rating)
            .add("totalAmount='" + totalAmount + "'")
            .add("unpaid='" + unpaid + "'")
            .toString();
    }
}
