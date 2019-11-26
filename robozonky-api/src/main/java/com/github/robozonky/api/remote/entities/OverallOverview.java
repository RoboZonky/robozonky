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

import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import io.vavr.Lazy;

public class OverallOverview extends BaseOverview {

    @XmlElement
    private String feesAmount;
    private final Lazy<Money> moneyFeesAmount = Lazy.of(() -> Money.from(feesAmount));
    @XmlElement
    private String netIncome;
    private final Lazy<Money> moneyNetIncome = Lazy.of(() -> Money.from(netIncome));
    @XmlElement
    private String principalLost;
    private final Lazy<Money> moneyPrincipalLost = Lazy.of(() -> Money.from(principalLost));
    @XmlElement
    private String feesDiscount;
    private final Lazy<Money> moneyFeesDiscount = Lazy.of(() -> Money.from(feesDiscount));

    OverallOverview() {
        // for JAXB
    }

    @XmlTransient
    public Money getFeesAmount() {
        return moneyFeesAmount.get();
    }

    @XmlTransient
    public Money getFeesDiscount() {
        return moneyFeesDiscount.get();
    }

    @XmlTransient
    public Money getNetIncome() {
        return moneyNetIncome.get();
    }

    @XmlTransient
    public Money getPrincipalLost() {
        return moneyPrincipalLost.get();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", OverallOverview.class.getSimpleName() + "[", "]")
                .add("super=" + super.toString())
                .add("feesAmount='" + feesAmount + "'")
                .add("feesDiscount='" + feesDiscount + "'")
                .add("netIncome='" + netIncome + "'")
                .add("principalLost='" + principalLost + "'")
                .toString();
    }
}
