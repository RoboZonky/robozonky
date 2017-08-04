/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import javax.xml.bind.annotation.XmlElement;

import com.github.triceo.robozonky.api.remote.enums.MainIncomeType;
import com.github.triceo.robozonky.api.remote.enums.Purpose;
import com.github.triceo.robozonky.api.remote.enums.Rating;

public class Participation {

    private OffsetDateTime deadline, nextPaymentDate;
    private int id, investmentId, loanId, originalInstalmentCount, remainingInstalmentCount, userId;
    private MainIncomeType incomeType;
    private double interestRate, remainingPrincipal;
    private String loanName;
    private Purpose purpose;
    private Rating rating;
    private boolean willExceedLoanInvestmentLimit;

    @XmlElement
    public OffsetDateTime getDeadline() {
        return deadline;
    }

    @XmlElement
    public OffsetDateTime getNextPaymentDate() {
        return nextPaymentDate;
    }

    @XmlElement
    public int getId() {
        return id;
    }

    @XmlElement
    public int getInvestmentId() {
        return investmentId;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public int getOriginalInstalmentCount() {
        return originalInstalmentCount;
    }

    @XmlElement
    public int getRemainingInstalmentCount() {
        return remainingInstalmentCount;
    }

    @XmlElement
    public int getUserId() {
        return userId;
    }

    @XmlElement
    public MainIncomeType getIncomeType() {
        return incomeType;
    }

    @XmlElement
    public double getInterestRate() {
        return interestRate;
    }

    @XmlElement
    public double getRemainingPrincipal() {
        return remainingPrincipal;
    }

    @XmlElement
    public String getLoanName() {
        return loanName;
    }

    @XmlElement
    public Purpose getPurpose() {
        return purpose;
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @XmlElement
    public boolean isWillExceedLoanInvestmentLimit() {
        return willExceedLoanInvestmentLimit;
    }
}
