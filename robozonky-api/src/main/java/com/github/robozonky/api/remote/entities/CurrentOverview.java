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

import java.util.Optional;
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import io.vavr.Lazy;

public class CurrentOverview extends BaseOverview {

    @XmlElement
    private long principalLeft;
    private final Lazy<Money> moneyPrincipalLeft = Lazy.of(() -> Money.from(principalLeft));
    @XmlElement
    private long principalLeftToPay;
    private final Lazy<Money> moneyPrincipalLeftToPay = Lazy.of(() -> Money.from(principalLeftToPay));
    @XmlElement
    private long principalLeftDue;
    private final Lazy<Money> moneyPrincipalLeftDue = Lazy.of(() -> Money.from(principalLeftDue));
    @XmlElement
    private long interestPlanned;
    private final Lazy<Money> moneyInterestPlanned = Lazy.of(() -> Money.from(interestPlanned));
    @XmlElement
    private long interestLeft;
    private final Lazy<Money> moneyInterestLeft = Lazy.of(() -> Money.from(interestLeft));
    @XmlElement
    private long interestLeftToPay;
    private final Lazy<Money> moneyInterestLeftToPay = Lazy.of(() -> Money.from(interestLeftToPay));
    @XmlElement
    private long interestLeftDue;
    private final Lazy<Money> moneyInterestLeftDue = Lazy.of(() -> Money.from(interestLeftDue));
    @XmlElement
    private CurrentPortfolio currentInvestments;

    CurrentOverview() {
        // for JAXB
    }

    @XmlTransient
    public Money getPrincipalLeft() {
        return moneyPrincipalLeft.get();
    }

    @XmlTransient
    public Money getPrincipalLeftToPay() {
        return moneyPrincipalLeftToPay.get();
    }

    @XmlTransient
    public Money getPrincipalLeftDue() {
        return moneyPrincipalLeftDue.get();
    }

    @XmlTransient
    public Money getInterestPlanned() {
        return moneyInterestPlanned.get();
    }

    @XmlTransient
    public Money getInterestLeft() {
        return moneyInterestLeft.get();
    }

    @XmlTransient
    public Money getInterestLeftToPay() {
        return moneyInterestLeftToPay.get();
    }

    @XmlTransient
    public Money getInterestLeftDue() {
        return moneyInterestLeftDue.get();
    }

    @XmlTransient
    public Optional<CurrentPortfolio> getCurrentInvestments() {
        return Optional.ofNullable(currentInvestments);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", CurrentOverview.class.getSimpleName() + "[", "]")
                .add("super=" + super.toString())
                .add("currentInvestments=" + currentInvestments)
                .add("interestLeft=" + interestLeft)
                .add("interestLeftDue=" + interestLeftDue)
                .add("interestLeftToPay=" + interestLeftToPay)
                .add("interestPlanned=" + interestPlanned)
                .add("principalLeft=" + principalLeft)
                .add("principalLeftDue=" + principalLeftDue)
                .add("principalLeftToPay=" + principalLeftToPay)
                .toString();
    }
}
