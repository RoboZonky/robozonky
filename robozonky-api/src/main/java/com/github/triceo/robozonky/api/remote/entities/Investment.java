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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import javax.xml.bind.annotation.XmlElement;

import com.github.triceo.robozonky.api.remote.enums.Rating;

public class Investment extends BaseInvestment {

    private int dpd, loanTermInMonth, currentTerm;
    private String loanName, nickname, firstName, surname, paymentStatus;
    private OffsetDateTime investmentDate, nextPaymentDate;
    private BigDecimal interestRate, paid, toPay, amountDue, paidInterest, dueInterest, paidPrincipal, duePrincipal,
            expectedInterest;
    private Rating rating;

    Investment() {
        // for JAXB
    }

    public Investment(final Loan loan, final int amount) {
        super(loan, amount);
        this.loanName = loan.getName();
        this.nickname = loan.getNickName();
        this.rating = loan.getRating();
        this.loanTermInMonth = loan.getTermInMonths();
        this.interestRate = loan.getInterestRate();
        this.currentTerm = this.loanTermInMonth;
        this.paymentStatus = "OK";
        this.paid = BigDecimal.ZERO;
        this.paidPrincipal = BigDecimal.ZERO;
        this.duePrincipal = BigDecimal.valueOf(amount);
        if (loan.getMyInvestment() != null) {
            this.investmentDate = loan.getMyInvestment().getTimeCreated();
        } else {
            this.investmentDate = OffsetDateTime.now();
        }
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @XmlElement
    public int getDpd() {
        return dpd;
    }

    @XmlElement
    public int getLoanTermInMonth() {
        return loanTermInMonth;
    }

    @XmlElement
    public int getCurrentTerm() {
        return currentTerm;
    }

    @XmlElement
    public String getLoanName() {
        return loanName;
    }

    @XmlElement
    public String getNickname() {
        return nickname;
    }

    @XmlElement
    public String getFirstName() {
        return firstName;
    }

    @XmlElement
    public String getSurname() {
        return surname;
    }

    @XmlElement
    // FIXME implement as enum, do not forget error-catching deserializer
    public String getPaymentStatus() {
        return paymentStatus;
    }

    @XmlElement
    public OffsetDateTime getInvestmentDate() {
        return investmentDate;
    }

    @XmlElement
    public OffsetDateTime getNextPaymentDate() {
        return nextPaymentDate;
    }

    @XmlElement
    public BigDecimal getInterestRate() {
        return interestRate;
    }

    @XmlElement
    public BigDecimal getPaid() {
        return paid;
    }

    @XmlElement
    public BigDecimal getToPay() {
        return toPay;
    }

    @XmlElement
    public BigDecimal getAmountDue() {
        return amountDue;
    }

    @XmlElement
    public BigDecimal getPaidInterest() {
        return paidInterest;
    }

    @XmlElement
    public BigDecimal getDueInterest() {
        return dueInterest;
    }

    @XmlElement
    public BigDecimal getPaidPrincipal() {
        return paidPrincipal;
    }

    @XmlElement
    public BigDecimal getDuePrincipal() {
        return duePrincipal;
    }

    @XmlElement
    public BigDecimal getExpectedInterest() {
        return expectedInterest;
    }
}
