/*
 * Copyright 2017 The RoboZonky Project
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
import java.time.OffsetDateTime;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;

public class Investment extends BaseInvestment {

    private static final BigDecimal SMP_FEE_RATE = new BigDecimal("0.015");
    private PaymentStatus paymentStatus;
    private boolean smpRelated, onSmp, canBeOffered, inWithdrawal, hasCollectionHistory;
    private int legalDpd, loanTermInMonth = 84, currentTerm = 0, remainingMonths = loanTermInMonth - currentTerm;
    private String loanName, nickname, firstName, surname;
    private OffsetDateTime investmentDate = OffsetDateTime.now(), nextPaymentDate = investmentDate.plusMonths(1),
            activeTo = investmentDate.plusMonths(loanTermInMonth);
    private BigDecimal interestRate, paid, toPay, amountDue, paidInterest = BigDecimal.ZERO, dueInterest, paidPrincipal,
            duePrincipal, expectedInterest, purchasePrice, remainingPrincipal, smpSoldFor,
            smpFee, paidPenalty = BigDecimal.ZERO;
    private Rating rating;

    Investment() {
        // for JAXB
    }

    public Investment(final Loan loan, final int amount) {
        super(loan, BigDecimal.valueOf(amount));
        this.legalDpd = 0;
        this.loanName = loan.getName();
        this.nickname = loan.getNickName();
        this.rating = loan.getRating();
        this.loanTermInMonth = loan.getTermInMonths();
        this.interestRate = loan.getInterestRate();
        this.currentTerm = this.loanTermInMonth;
        this.remainingMonths = loan.getTermInMonths();
        this.remainingPrincipal = BigDecimal.valueOf(amount);
        this.smpFee = remainingPrincipal.multiply(SMP_FEE_RATE);
        this.paymentStatus = PaymentStatus.OK;
        this.paid = BigDecimal.ZERO;
        this.paidPrincipal = BigDecimal.ZERO;
        this.duePrincipal = BigDecimal.valueOf(amount);
        this.purchasePrice = this.duePrincipal;
        this.canBeOffered = false;
        this.onSmp = false;
        this.smpRelated = false;
        this.activeTo = OffsetDateTime.MAX;
    }

    public Investment(final ParticipationDescriptor participationDescriptor) {
        super(participationDescriptor.related(), participationDescriptor.item().getRemainingPrincipal());
        final Loan loan = participationDescriptor.related();
        final Participation participation = participationDescriptor.item();
        this.legalDpd = 0;
        this.loanName = loan.getName();
        this.nickname = loan.getNickName();
        this.rating = loan.getRating();
        this.loanTermInMonth = loan.getTermInMonths();
        this.interestRate = loan.getInterestRate();
        this.currentTerm = loan.getTermInMonths();
        this.remainingMonths = participation.getRemainingInstalmentCount();
        this.remainingPrincipal = participation.getRemainingPrincipal();
        this.smpFee = remainingPrincipal.multiply(SMP_FEE_RATE);
        this.paymentStatus = PaymentStatus.OK;
        this.paid = BigDecimal.ZERO;
        this.paidPrincipal = BigDecimal.ZERO;
        this.duePrincipal = BigDecimal.ZERO;
        this.purchasePrice = participation.getRemainingPrincipal();
        this.canBeOffered = true;
        this.onSmp = false;
        this.smpRelated = false;
        this.activeTo = OffsetDateTime.MAX;
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @XmlElement
    public int getLegalDpd() {
        return legalDpd;
    }

    /**
     * The original term.
     * @return
     */
    @XmlElement
    public int getLoanTermInMonth() {
        return loanTermInMonth;
    }

    /**
     * How many monthly payments were remaining to be made from {@link #getLoanTermInMonth()}.
     * May be less than {@link #getRemainingMonths()} in case of early payments.
     * @return
     */
    @XmlElement
    public int getCurrentTerm() {
        return currentTerm;
    }

    @XmlElement
    public boolean isSmpRelated() {
        return smpRelated;
    }

    @XmlElement
    public boolean isOnSmp() {
        return onSmp;
    }

    @XmlElement
    public boolean isCanBeOffered() {
        return canBeOffered;
    }

    /**
     * The client terminated the loan contract. The investment can therefore not be sold on secondary marketplace.
     * @return
     */
    @XmlElement
    public boolean isInWithdrawal() {
        return inWithdrawal;
    }

    /**
     * How many monthly payments are now remaining. Also see {@link #getCurrentTerm()}.
     * @return
     */
    @XmlElement
    public int getRemainingMonths() {
        return remainingMonths;
    }

    @XmlElement
    public BigDecimal getPurchasePrice() {
        return purchasePrice;
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
    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    @XmlElement
    public OffsetDateTime getInvestmentDate() {
        return investmentDate;
    }

    /**
     * In case of a presently delinquent loan, this always shows the date of the least recent instalment that is
     * delinquent.
     * @return
     */
    @XmlElement
    public OffsetDateTime getNextPaymentDate() {
        return nextPaymentDate;
    }

    @XmlElement
    public OffsetDateTime getActiveTo() {
        return activeTo;
    }

    @XmlElement
    public boolean hasCollectionHistory() {
        return hasCollectionHistory;
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

    @XmlElement
    public BigDecimal getRemainingPrincipal() {
        return remainingPrincipal;
    }

    @XmlElement
    public BigDecimal getSmpSoldFor() {
        return smpSoldFor;
    }

    @XmlElement
    public BigDecimal getPaidPenalty() {
        return paidPenalty;
    }

    @XmlElement
    public BigDecimal getSmpFee() {
        return smpFee;
    }

    public void setIsOnSmp(final boolean isOnSmp) {
        this.onSmp = isOnSmp;
    }
}
