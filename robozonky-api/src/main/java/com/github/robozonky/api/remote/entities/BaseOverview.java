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
import io.vavr.Lazy;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

abstract class BaseOverview extends BaseEntity {

    private long investmentCount;

    // strings to be represented as money

    @XmlElement
    private long totalInvestment;
    private final Lazy<Money> moneyTotalInvestment = Lazy.of(() -> Money.from(totalInvestment));
    @XmlElement
    private long principalPaid;
    private final Lazy<Money> moneyPrincipalPaid = Lazy.of(() -> Money.from(principalPaid));
    @XmlElement
    private long interestPaid;
    private final Lazy<Money> moneyInterestPaid = Lazy.of(() -> Money.from(interestPaid));
    @XmlElement
    private long penaltyPaid;
    private final Lazy<Money> moneyPenaltyPaid = Lazy.of(() -> Money.from(penaltyPaid));

    @XmlElement
    public long getInvestmentCount() {
        return investmentCount;
    }

    // money-based fields are all transient

    @XmlTransient
    public Money getTotalInvestment() {
        return moneyTotalInvestment.get();
    }

    @XmlTransient
    public Money getPrincipalPaid() {
        return moneyPrincipalPaid.get();
    }

    @XmlTransient
    public Money getInterestPaid() {
        return moneyInterestPaid.get();
    }

    @XmlTransient
    public Money getPenaltyPaid() {
        return moneyPenaltyPaid.get();
    }

}
