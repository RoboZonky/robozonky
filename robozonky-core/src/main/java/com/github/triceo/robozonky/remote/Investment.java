/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.robozonky.remote;

import java.math.BigDecimal;
import java.time.Instant;
import javax.xml.bind.annotation.XmlElement;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class Investment {

    private int id, loanId, amount, dpd, loanTermInMonth, currentTerm;
    private String loanName, nickname, firstName, surname, paymentStatus;
    private Instant investmentDate, nextPaymentDate;
    private BigDecimal interestRate, paid, toPay, amountDue, paidInterest, dueInterest, paidPrincipal, duePrincipal, expectedInterest;
    private Rating rating;

    Investment() {
        // for JAXB
    }

    public Investment(final Loan loan, final int amount) {
        this.loanId = loan.getId();
        this.loanName = loan.getName();
        this.nickname = loan.getNickName();
        this.rating = loan.getRating();
        this.loanTermInMonth = loan.getTermInMonths();
        this.interestRate = loan.getInterestRate();
        this.amount = amount;
        this.currentTerm = this.loanTermInMonth;
        this.paymentStatus = "OK";
        this.investmentDate = Instant.now();
        this.paid = BigDecimal.ZERO;
        this.paidPrincipal = BigDecimal.ZERO;
        this.duePrincipal = BigDecimal.valueOf(amount);
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @XmlElement
    public int getLoanId() {
        return loanId;
    }

    @XmlElement
    public int getAmount() {
        return amount;
    }

    @XmlElement
    public int getId() {
        return id;
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
    public String getPaymentStatus() {
        return paymentStatus;
    }

    @XmlElement
    @JsonDeserialize(using = InstantDeserializer.class)
    public Instant getInvestmentDate() {
        return investmentDate;
    }

    @XmlElement
    @JsonDeserialize(using = InstantDeserializer.class)
    public Instant getNextPaymentDate() {
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

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("FullInvestment{");
        sb.append("id=").append(id);
        sb.append(", loanId=").append(this.getLoanId());
        sb.append(", loanName='").append(loanName).append('\'');
        sb.append(", amount=").append(this.getAmount());
        sb.append(", rating=").append(this.getRating());
        sb.append(", interestRate=").append(interestRate);
        sb.append(", loanTermInMonth=").append(loanTermInMonth);
        sb.append(", currentTerm=").append(currentTerm);
        sb.append(", dpd=").append(dpd);
        sb.append(", paymentStatus='").append(paymentStatus).append('\'');
        sb.append(", amountDue=").append(amountDue);
        sb.append('}');
        return sb.toString();
    }
}
