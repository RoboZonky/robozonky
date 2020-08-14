/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.remote.entities;

import java.time.Instant;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.InsuranceStatus;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

public class InvestmentImpl implements Investment {

    private long id;
    private int loanId;
    private Money amount;
    private InvestmentStatus status;

    @JsonbProperty(nillable = true)
    private PaymentStatus paymentStatus;
    @JsonbProperty(nillable = true)
    private LoanHealth loanHealthInfo;
    private boolean onSmp;
    private boolean insuranceActive;
    private boolean instalmentPostponement;
    @JsonbProperty(nillable = true)
    private Integer legalDpd;
    @JsonbProperty(nillable = true)
    private Integer currentTerm = 0;
    private int loanTermInMonth = 84;
    private int remainingMonths = loanTermInMonth - currentTerm;
    private String loanName;
    private InsuranceStatus insuranceStatus = InsuranceStatus.NOT_INSURED;
    private Ratio interestRate;
    @JsonbProperty(nillable = true)
    private Ratio revenueRate;
    private Rating rating;

    private Money loanAnnuity = Money.ZERO;
    private Money loanAmount = Money.ZERO;
    @JsonbProperty(nillable = true)
    private Money paid = Money.ZERO;
    @JsonbProperty(nillable = true)
    private Money toPay = Money.ZERO;
    @JsonbProperty(nillable = true)
    private Money amountDue = Money.ZERO;
    private Money paidInterest = Money.ZERO;
    private Money dueInterest = Money.ZERO;
    private Money paidPrincipal = Money.ZERO;
    private Money duePrincipal = Money.ZERO;
    private Money expectedInterest = Money.ZERO;
    private Money purchasePrice = Money.ZERO;
    @JsonbProperty(nillable = true)
    private Money remainingPrincipal = Money.ZERO;
    @JsonbProperty(nillable = true)
    private Money smpPrice = Money.ZERO;
    @JsonbProperty(nillable = true)
    private Money smpSoldFor = Money.ZERO;
    @JsonbProperty(nillable = true)
    private Money smpFee = Money.ZERO;
    private Money paidPenalty = Money.ZERO;

    public InvestmentImpl() {
        // For JSON-B.
    }

    public InvestmentImpl(final Loan loan, final Money amount) {
        this.loanId = loan.getId();
        this.amount = amount;
        this.status = InvestmentStatus.ACTIVE;
        this.rating = loan.getRating();
        this.interestRate = rating.getInterestRate();
        this.revenueRate = rating.getMinimalRevenueRate(Instant.now());
        this.remainingPrincipal = amount;
        this.purchasePrice = remainingPrincipal;
        this.remainingMonths = loan.getTermInMonths();
        this.loanTermInMonth = loan.getTermInMonths();
        this.insuranceActive = loan.isInsuranceActive();
    }

    @Override
    public InvestmentStatus getStatus() {
        return status;
    }

    public void setStatus(final InvestmentStatus status) {
        this.status = status;
    }

    @Override
    public int getLoanId() {
        return loanId;
    }

    public void setLoanId(final int loanId) {
        this.loanId = loanId;
    }

    @Override
    public long getId() {
        return id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    @Override
    public Money getAmount() {
        return amount;
    }

    public void setAmount(final Money amount) {
        this.amount = amount;
    }

    @Override
    public Rating getRating() {
        return rating;
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }

    @Override
    public Optional<LoanHealth> getLoanHealthInfo() {
        return Optional.ofNullable(loanHealthInfo);
    }

    public void setLoanHealthInfo(final LoanHealth loanHealthInfo) {
        this.loanHealthInfo = loanHealthInfo;
    }

    @Override
    public OptionalInt getLegalDpd() {
        return legalDpd == null ? OptionalInt.empty() : OptionalInt.of(legalDpd);
    }

    public void setLegalDpd(final Integer legalDpd) {
        this.legalDpd = legalDpd;
    }

    @Override
    public int getLoanTermInMonth() {
        return loanTermInMonth;
    }

    public void setLoanTermInMonth(final int loanTermInMonth) {
        this.loanTermInMonth = loanTermInMonth;
    }

    @Override
    public OptionalInt getCurrentTerm() {
        return currentTerm == null ? OptionalInt.empty() : OptionalInt.of(currentTerm);
    }

    public void setCurrentTerm(final Integer currentTerm) {
        this.currentTerm = currentTerm;
    }

    @Override
    public boolean isOnSmp() {
        return onSmp;
    }

    public void setOnSmp(final boolean onSmp) {
        this.onSmp = onSmp;
    }

    @Override
    public int getRemainingMonths() {
        return remainingMonths;
    }

    public void setRemainingMonths(final int remainingMonths) {
        this.remainingMonths = remainingMonths;
    }

    @Override
    public String getLoanName() {
        return loanName;
    }

    public void setLoanName(final String loanName) {
        this.loanName = loanName;
    }

    @Override
    public Optional<PaymentStatus> getPaymentStatus() {
        return Optional.ofNullable(paymentStatus);
    }

    public void setPaymentStatus(final PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    @Override
    public Ratio getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    @Override
    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    public void setRevenueRate(final Ratio revenueRate) {
        this.revenueRate = revenueRate;
    }

    @Override
    public InsuranceStatus getInsuranceStatus() {
        return insuranceStatus;
    }

    public void setInsuranceStatus(final InsuranceStatus insuranceStatus) {
        this.insuranceStatus = insuranceStatus;
    }

    @Override
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    public void setInsuranceActive(final boolean insuranceActive) {
        this.insuranceActive = insuranceActive;
    }

    @Override
    public boolean isInstalmentPostponement() {
        return instalmentPostponement;
    }

    public void setInstalmentPostponement(final boolean instalmentPostponement) {
        this.instalmentPostponement = instalmentPostponement;
    }

    @Override
    public Money getLoanAnnuity() {
        return loanAnnuity;
    }

    public void setLoanAnnuity(final Money loanAnnuity) {
        this.loanAnnuity = loanAnnuity;
    }

    @Override
    public Money getLoanAmount() {
        return loanAmount;
    }

    public void setLoanAmount(final Money loanAmount) {
        this.loanAmount = loanAmount;
    }

    @Override
    public Money getPurchasePrice() {
        return purchasePrice;
    }

    public void setPurchasePrice(final Money purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    @Override
    public Money getPaid() {
        return paid;
    }

    public void setPaid(final Money paid) {
        this.paid = paid;
    }

    @Override
    public Money getToPay() {
        return toPay;
    }

    public void setToPay(final Money toPay) {
        this.toPay = toPay;
    }

    @Override
    public Money getAmountDue() {
        return amountDue;
    }

    public void setAmountDue(final Money amountDue) {
        this.amountDue = amountDue;
    }

    @Override
    public Money getPaidInterest() {
        return paidInterest;
    }

    public void setPaidInterest(final Money paidInterest) {
        this.paidInterest = paidInterest;
    }

    @Override
    public Money getDueInterest() {
        return dueInterest;
    }

    public void setDueInterest(final Money dueInterest) {
        this.dueInterest = dueInterest;
    }

    @Override
    public Money getPaidPrincipal() {
        return paidPrincipal;
    }

    public void setPaidPrincipal(final Money paidPrincipal) {
        this.paidPrincipal = paidPrincipal;
    }

    @Override
    public Money getDuePrincipal() {
        return duePrincipal;
    }

    public void setDuePrincipal(final Money duePrincipal) {
        this.duePrincipal = duePrincipal;
    }

    @Override
    public Money getExpectedInterest() {
        return expectedInterest;
    }

    public void setExpectedInterest(final Money expectedInterest) {
        this.expectedInterest = expectedInterest;
    }

    @Override
    public Optional<Money> getRemainingPrincipal() {
        return Optional.ofNullable(remainingPrincipal);
    }

    public void setRemainingPrincipal(final Money remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    @Override
    public Optional<Money> getSmpSoldFor() {
        return Optional.ofNullable(smpSoldFor);
    }

    public void setSmpSoldFor(final Money smpSoldFor) {
        this.smpSoldFor = smpSoldFor;
    }

    @Override
    public Money getPaidPenalty() {
        return paidPenalty;
    }

    public void setPaidPenalty(final Money paidPenalty) {
        this.paidPenalty = paidPenalty;
    }

    @Override
    public Optional<Money> getSmpFee() {
        return Optional.ofNullable(smpFee);
    }

    public void setSmpFee(final Money smpFee) {
        this.smpFee = smpFee;
    }

    @Override
    public Optional<Money> getSmpPrice() {
        return Optional.ofNullable(smpPrice);
    }

    public void setSmpPrice(final Money smpPrice) {
        this.smpPrice = smpPrice;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InvestmentImpl.class.getSimpleName() + "[", "]")
            .add("id=" + id)
            .add("amount=" + amount)
            .add("amountDue=" + amountDue)
            .add("currentTerm=" + currentTerm)
            .add("dueInterest=" + dueInterest)
            .add("duePrincipal=" + duePrincipal)
            .add("expectedInterest=" + expectedInterest)
            .add("instalmentPostponement=" + instalmentPostponement)
            .add("insuranceActive=" + insuranceActive)
            .add("insuranceStatus=" + insuranceStatus)
            .add("interestRate=" + interestRate)
            .add("legalDpd=" + legalDpd)
            .add("loanAmount=" + loanAmount)
            .add("loanAnnuity=" + loanAnnuity)
            .add("loanHealthInfo=" + loanHealthInfo)
            .add("loanId=" + loanId)
            .add("loanName='" + loanName + "'")
            .add("loanTermInMonth=" + loanTermInMonth)
            .add("onSmp=" + onSmp)
            .add("paid=" + paid)
            .add("paidInterest=" + paidInterest)
            .add("paidPenalty=" + paidPenalty)
            .add("paidPrincipal=" + paidPrincipal)
            .add("paymentStatus=" + paymentStatus)
            .add("purchasePrice=" + purchasePrice)
            .add("rating=" + rating)
            .add("remainingMonths=" + remainingMonths)
            .add("remainingPrincipal=" + remainingPrincipal)
            .add("revenueRate=" + revenueRate)
            .add("smpFee=" + smpFee)
            .add("smpPrice=" + smpPrice)
            .add("smpSoldFor=" + smpSoldFor)
            .add("status=" + status)
            .add("toPay=" + toPay)
            .toString();
    }
}
