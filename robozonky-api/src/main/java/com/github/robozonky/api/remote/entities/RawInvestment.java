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

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.function.Function;
import javax.xml.bind.annotation.XmlElement;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.InsuranceStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.util.DateUtil;

/**
 * It is not recommended to use this class directly as Zonky will return various null references for fields at various
 * points in the investment lifecycle. Please use {@link Investment} as a null-safe alternative. Instances may be
 * created with static methods such as {@link Investment#sanitized(RawInvestment, Function)} )}.
 */
public class RawInvestment extends BaseInvestment {

    private PaymentStatus paymentStatus;
    private boolean smpRelated, onSmp, canBeOffered, inWithdrawal, hasCollectionHistory, insuranceActive,
            instalmentPostponement;
    private int legalDpd, loanTermInMonth = 84, currentTerm = 0, remainingMonths = loanTermInMonth - currentTerm;
    private String loanName, nickname, firstName, surname;
    private InsuranceStatus insuranceStatus = InsuranceStatus.NOT_INSURED;
    private OffsetDateTime investmentDate = DateUtil.offsetNow();
    private OffsetDateTime nextPaymentDate = investmentDate.plusMonths(1);
    private OffsetDateTime activeTo;
    private BigDecimal interestRate, paid, toPay, amountDue, paidInterest = BigDecimal.ZERO, dueInterest, paidPrincipal,
            duePrincipal, expectedInterest, purchasePrice, remainingPrincipal, smpSoldFor,
            smpFee, paidPenalty = BigDecimal.ZERO;
    private BigDecimal revenueRate;
    private Rating rating;
    private Collection<InsurancePolicyPeriod> insuranceHistory;

    RawInvestment() {
        // for JAXB
    }

    public RawInvestment(final Investment investment, final Loan loan) {
        this(investment);
        this.loanName = loan.getName();
        this.nickname = loan.getNickName();
    }

    public RawInvestment(final Investment investment) {
        super(investment);
        this.paymentStatus = investment.getPaymentStatus().orElse(null);
        this.onSmp = investment.isOnSmp();
        this.canBeOffered = investment.canBeOffered();
        this.inWithdrawal = investment.isInWithdrawal().orElse(false);
        this.legalDpd = investment.getDaysPastDue();
        this.loanTermInMonth = investment.getOriginalTerm();
        this.remainingMonths = investment.getRemainingMonths();
        this.currentTerm = investment.getCurrentTerm();
        this.investmentDate = investment.getInvestmentDate();
        this.nextPaymentDate = investment.getNextPaymentDate().orElse(null);
        this.interestRate = investment.getInterestRate();
        this.revenueRate = investment.getRevenueRate();
        this.paidInterest = investment.getPaidInterest();
        this.dueInterest = investment.getDueInterest();
        this.paidPrincipal = investment.getPaidPrincipal();
        this.duePrincipal = investment.getDuePrincipal();
        this.expectedInterest = investment.getExpectedInterest();
        this.purchasePrice = investment.getOriginalPrincipal();
        this.remainingPrincipal = investment.getRemainingPrincipal();
        this.smpSoldFor = investment.getSmpSoldFor().orElse(null);
        this.smpFee = investment.getSmpFee().orElse(null);
        this.paidPenalty = investment.getPaidPenalty();
        this.rating = investment.getRating();
        this.hasCollectionHistory = false;
        this.insuranceStatus =
                investment.isInsuranceActive() ? InsuranceStatus.CURRENTLY_INSURED : InsuranceStatus.NOT_INSURED;
        this.insuranceActive = investment.isInsuranceActive();
        this.instalmentPostponement = investment.areInstalmentsPostponed();
        this.insuranceHistory = investment.getInsuranceHistory();
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @XmlElement
    public Integer getLegalDpd() {
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
     * @return Null for loans where no payments are expected anymore.
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

    /**
     * @return Null when the investment is already sold.
     */
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

    @XmlElement
    public InsuranceStatus getInsuranceStatus() {
        return insuranceStatus;
    }

    @XmlElement
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @XmlElement
    public boolean isInstalmentPostponement() {
        return instalmentPostponement;
    }

    @XmlElement
    public Collection<InsurancePolicyPeriod> getInsuranceHistory() {
        return insuranceHistory;
    }

    @XmlElement
    public boolean hasCollectionHistory() {
        return hasCollectionHistory;
    }

    @XmlElement
    public BigDecimal getRevenueRate() {
        return revenueRate;
    }
}
