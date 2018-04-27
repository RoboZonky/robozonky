/*
 * Copyright 2018 The RoboZonky Project
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

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Collections;
import javax.xml.bind.annotation.XmlElement;

public class BorrowerRelatedInvestmentInfo extends BaseEntity {

    private BigDecimal totalPrincipalToLoan, remainingPrincipalToLoan, totalPrincipalToBorrower,
            remainingPrincipalToBorrower, totalSoldAmountToLoan;
    private int activeCountToLoan, soldCountToLoan;

    private Collection<String> otherBorrowerNicknames = Collections.emptyList();

    BorrowerRelatedInvestmentInfo() {
        // for JAXB
    }

    /**
     * @return Will be null if no other nicknames.
     */
    @XmlElement
    public Collection<String> getOtherBorrowerNicknames() {
        return otherBorrowerNicknames;
    }

    @XmlElement
    public BigDecimal getTotalPrincipalToLoan() {
        return totalPrincipalToLoan;
    }

    @XmlElement
    public BigDecimal getRemainingPrincipalToLoan() {
        return remainingPrincipalToLoan;
    }

    /**
     * @return Will be null if no other loans to the borrower.
     */
    @XmlElement
    public BigDecimal getTotalPrincipalToBorrower() {
        return totalPrincipalToBorrower;
    }

    /**
     * @return Will be null if no other loans to the borrower.
     */
    @XmlElement
    public BigDecimal getRemainingPrincipalToBorrower() {
        return remainingPrincipalToBorrower;
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
