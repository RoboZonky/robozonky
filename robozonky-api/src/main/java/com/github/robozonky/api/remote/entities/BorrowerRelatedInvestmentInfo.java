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

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import io.vavr.Lazy;

public class BorrowerRelatedInvestmentInfo extends BaseEntity {

    private int activeCountToLoan;
    private int soldCountToLoan;

    @XmlElement
    private Collection<String> otherBorrowerNicknames = Collections.emptyList();

    // string representation of money

    @XmlElement
    private String totalPrincipalToLoan;
    private final Lazy<Money> moneyTotalPrincipalToLoan = Lazy.of(() -> Money.from(totalPrincipalToLoan));
    @XmlElement
    private String remainingPrincipalToLoan;
    private final Lazy<Money> moneyRemainingPrincipalToLoan = Lazy.of(() -> Money.from(remainingPrincipalToLoan));
    @XmlElement
    private String totalPrincipalToBorrower;
    private final Lazy<Money> moneyTotalPrincipalToBorrower = Lazy.of(() -> Money.from(totalPrincipalToBorrower));
    @XmlElement
    private String remainingPrincipalToBorrower;
    private final Lazy<Money> moneyRemainingPrincipalToBorrower = Lazy.of(() -> Money.from(remainingPrincipalToBorrower));
    @XmlElement
    private String totalSoldAmountToLoan;
    private final Lazy<Money> moneyTotalSoldAmountToLoan = Lazy.of(() -> Money.from(totalSoldAmountToLoan));

    BorrowerRelatedInvestmentInfo() {
        // for JAXB
    }

    /**
     * @return Will be null if no other nicknames.
     */
    public Collection<String> getOtherBorrowerNicknames() {
        return otherBorrowerNicknames == null ? Collections.emptySet() : otherBorrowerNicknames;
    }

    @XmlElement
    public int getActiveCountToLoan() {
        return activeCountToLoan;
    }

    @XmlElement
    public int getSoldCountToLoan() {
        return soldCountToLoan;
    }

    // money-based fields are all transient

    @XmlTransient
    public Optional<Money> getTotalPrincipalToLoan() {
        return Optional.ofNullable(moneyTotalPrincipalToLoan.getOrElse((Money) null));
    }

    @XmlTransient
    public Optional<Money> getRemainingPrincipalToLoan() {
        return Optional.ofNullable(moneyRemainingPrincipalToLoan.getOrElse((Money) null));
    }

    /**
     * @return Empty if no other loans to the borrower.
     */
    @XmlTransient
    public Optional<Money> getTotalPrincipalToBorrower() {
        return Optional.ofNullable(moneyTotalPrincipalToBorrower.getOrElse((Money) null));
    }

    /**
     * @return Empty if no other loans to the borrower.
     */
    @XmlTransient
    public Optional<Money> getRemainingPrincipalToBorrower() {
        return Optional.ofNullable(moneyRemainingPrincipalToBorrower.getOrElse((Money) null));
    }

    @XmlTransient
    public Money getTotalSoldAmountToLoan() {
        return moneyTotalSoldAmountToLoan.get();
    }

}
