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

import javax.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

public class BorrowerRelatedInvestmentInfo extends BaseEntity {

    @XmlElement
    private BigDecimal totalPrincipalToLoan = BigDecimal.ZERO;
    @XmlElement
    private BigDecimal remainingPrincipalToLoan = BigDecimal.ZERO;;
    @XmlElement
    private BigDecimal totalPrincipalToBorrower = BigDecimal.ZERO;;
    @XmlElement
    private BigDecimal remainingPrincipalToBorrower = BigDecimal.ZERO;;
    private BigDecimal totalSoldAmountToLoan = BigDecimal.ZERO;;
    private int activeCountToLoan;
    private int soldCountToLoan;

    @XmlElement
    private Collection<String> otherBorrowerNicknames = Collections.emptyList();

    BorrowerRelatedInvestmentInfo() {
        // for JAXB
    }

    /**
     * @return Will be null if no other nicknames.
     */
    public Collection<String> getOtherBorrowerNicknames() {
        return otherBorrowerNicknames == null ? Collections.emptySet() : otherBorrowerNicknames;
    }

    public Optional<BigDecimal> getTotalPrincipalToLoan() {
        return Optional.ofNullable(totalPrincipalToLoan);
    }

    public Optional<BigDecimal> getRemainingPrincipalToLoan() {
        return Optional.ofNullable(remainingPrincipalToLoan);
    }

    /**
     * @return Empty if no other loans to the borrower.
     */
    public Optional<BigDecimal> getTotalPrincipalToBorrower() {
        return Optional.ofNullable(totalPrincipalToBorrower);
    }

    /**
     * @return Empty if no other loans to the borrower.
     */
    public Optional<BigDecimal> getRemainingPrincipalToBorrower() {
        return Optional.ofNullable(remainingPrincipalToBorrower);
    }

    @XmlElement
    public BigDecimal getTotalSoldAmountToLoan() {
        return totalSoldAmountToLoan;
    }

    @XmlElement
    public int getActiveCountToLoan() {
        return activeCountToLoan;
    }

    @XmlElement
    public int getSoldCountToLoan() {
        return soldCountToLoan;
    }
}
