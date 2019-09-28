/*
 * Copyright 2019 The RoboZonky Project
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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.enums.Rating;
import io.vavr.Lazy;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

public class RiskPortfolio extends BaseEntity {

    @XmlElement
    private String unpaid;
    private final Lazy<Money> moneyUnpaid = Lazy.of(() -> Money.from(unpaid));
    @XmlElement
    private String paid;
    private final Lazy<Money> moneyPaid = Lazy.of(() -> Money.from(paid));
    @XmlElement
    private String due;
    private final Lazy<Money> moneyDue = Lazy.of(() -> Money.from(due));
    @XmlElement
    private String totalAmount;
    private final Lazy<Money> moneyTotalAmount = Lazy.of(() -> Money.from(totalAmount));
    private Rating rating;

    RiskPortfolio() {
        // for JAXB
    }

    public RiskPortfolio(final Rating rating, final Money paid, final Money unpaid, final Money due) {
        this.paid = paid.getValue().toPlainString();
        this.unpaid = unpaid.getValue().toPlainString();
        this.due = due.getValue().toPlainString();
        this.rating = rating;
        this.totalAmount = paid.add(unpaid).add(due).getValue().toPlainString();
    }

    @XmlTransient
    public Money getUnpaid() {
        return moneyUnpaid.get();
    }

    @XmlTransient
    public Money getPaid() {
        return moneyPaid.get();
    }

    @XmlTransient
    public Money getDue() {
        return moneyDue.get();
    }

    @XmlTransient
    public Money getTotalAmount() {
        return moneyTotalAmount.get();
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }
}
