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
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.StringJoiner;

import javax.json.bind.annotation.JsonbProperty;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.InsuranceStatus;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

public class InvestmentImpl extends BaseInvestmentImpl implements Investment {

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
        super(loan, amount);
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
    public Rating getRating() {
        return rating;
    }

    @Override
    public Optional<LoanHealth> getLoanHealthInfo() {
        return Optional.ofNullable(loanHealthInfo);
    }

    @Override
    public OptionalInt getLegalDpd() {
        return legalDpd == null ? OptionalInt.empty() : OptionalInt.of(legalDpd);
    }

    @Override
    public int getLoanTermInMonth() {
        return loanTermInMonth;
    }

    @Override
    public OptionalInt getCurrentTerm() {
        return currentTerm == null ? OptionalInt.empty() : OptionalInt.of(currentTerm);
    }

    @Override
    public boolean isOnSmp() {
        return onSmp;
    }

    @Override
    public int getRemainingMonths() {
        return remainingMonths;
    }

    @Override
    public String getLoanName() {
        return loanName;
    }

    @Override
    public Optional<PaymentStatus> getPaymentStatus() {
        return Optional.ofNullable(paymentStatus);
    }

    @Override
    public Ratio getInterestRate() {
        return interestRate;
    }

    @Override
    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    @Override
    public InsuranceStatus getInsuranceStatus() {
        return insuranceStatus;
    }

    @Override
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @Override
    public boolean isInstalmentPostponement() {
        return instalmentPostponement;
    }

    @Override
    public Money getLoanAnnuity() {
        return loanAnnuity;
    }

    @Override
    public Money getLoanAmount() {
        return loanAmount;
    }

    @Override
    public Money getPurchasePrice() {
        return purchasePrice;
    }

    @Override
    public Money getPaid() {
        return paid;
    }

    @Override
    public Money getToPay() {
        return toPay;
    }

    @Override
    public Money getAmountDue() {
        return amountDue;
    }

    @Override
    public Money getPaidInterest() {
        return paidInterest;
    }

    @Override
    public Money getDueInterest() {
        return dueInterest;
    }

    @Override
    public Money getPaidPrincipal() {
        return paidPrincipal;
    }

    @Override
    public Money getDuePrincipal() {
        return duePrincipal;
    }

    @Override
    public Money getExpectedInterest() {
        return expectedInterest;
    }

    @Override
    public Optional<Money> getRemainingPrincipal() {
        return Optional.ofNullable(remainingPrincipal);
    }

    @Override
    public Optional<Money> getSmpSoldFor() {
        return Optional.ofNullable(smpSoldFor);
    }

    @Override
    public Money getPaidPenalty() {
        return paidPenalty;
    }

    @Override
    public Optional<Money> getSmpFee() {
        return Optional.ofNullable(smpFee);
    }

    @Override
    public Optional<Money> getSmpPrice() {
        return Optional.ofNullable(smpPrice);
    }

    public void setPaymentStatus(final PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
    }

    public void setLoanHealthInfo(final LoanHealth loanHealthInfo) {
        this.loanHealthInfo = loanHealthInfo;
    }

    public void setOnSmp(final boolean onSmp) {
        this.onSmp = onSmp;
    }

    public void setInsuranceActive(final boolean insuranceActive) {
        this.insuranceActive = insuranceActive;
    }

    public void setInstalmentPostponement(final boolean instalmentPostponement) {
        this.instalmentPostponement = instalmentPostponement;
    }

    public void setLegalDpd(final Integer legalDpd) {
        this.legalDpd = legalDpd;
    }

    public void setLoanTermInMonth(final int loanTermInMonth) {
        this.loanTermInMonth = loanTermInMonth;
    }

    public void setCurrentTerm(final Integer currentTerm) {
        this.currentTerm = currentTerm;
    }

    public void setRemainingMonths(final int remainingMonths) {
        this.remainingMonths = remainingMonths;
    }

    public void setLoanName(final String loanName) {
        this.loanName = loanName;
    }

    public void setInsuranceStatus(final InsuranceStatus insuranceStatus) {
        this.insuranceStatus = insuranceStatus;
    }

    public void setInterestRate(final Ratio interestRate) {
        this.interestRate = interestRate;
    }

    public void setRevenueRate(final Ratio revenueRate) {
        this.revenueRate = revenueRate;
    }

    public void setRating(final Rating rating) {
        this.rating = rating;
    }

    private static String toOptionalString(final OffsetDateTime dateTime) {
        return Optional.ofNullable(dateTime)
            .map(OffsetDateTime::toString)
            .orElse(null);
    }

    public void setLoanAnnuity(final Money loanAnnuity) {
        this.loanAnnuity = loanAnnuity;
    }

    public void setLoanAmount(final Money loanAmount) {
        this.loanAmount = loanAmount;
    }

    public void setPaid(final Money paid) {
        this.paid = paid;
    }

    public void setToPay(final Money toPay) {
        this.toPay = toPay;
    }

    public void setAmountDue(final Money amountDue) {
        this.amountDue = amountDue;
    }

    public void setPaidInterest(final Money paidInterest) {
        this.paidInterest = paidInterest;
    }

    public void setDueInterest(final Money dueInterest) {
        this.dueInterest = dueInterest;
    }

    public void setPaidPrincipal(final Money paidPrincipal) {
        this.paidPrincipal = paidPrincipal;
    }

    public void setDuePrincipal(final Money duePrincipal) {
        this.duePrincipal = duePrincipal;
    }

    public void setExpectedInterest(final Money expectedInterest) {
        this.expectedInterest = expectedInterest;
    }

    public void setPurchasePrice(final Money purchasePrice) {
        this.purchasePrice = purchasePrice;
    }

    public void setRemainingPrincipal(final Money remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
    }

    public void setSmpPrice(final Money smpPrice) {
        this.smpPrice = smpPrice;
    }

    public void setSmpSoldFor(final Money smpSoldFor) {
        this.smpSoldFor = smpSoldFor;
    }

    public void setSmpFee(final Money smpFee) {
        this.smpFee = smpFee;
    }

    public void setPaidPenalty(final Money paidPenalty) {
        this.paidPenalty = paidPenalty;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", InvestmentImpl.class.getSimpleName() + "[", "]")
            .add("super=" + super.toString())
            .add("amountDue='" + amountDue + "'")
            .add("currentTerm=" + currentTerm)
            .add("dueInterest='" + dueInterest + "'")
            .add("duePrincipal='" + duePrincipal + "'")
            .add("expectedInterest='" + expectedInterest + "'")
            .add("instalmentPostponement=" + instalmentPostponement)
            .add("insuranceActive=" + insuranceActive)
            .add("insuranceStatus=" + insuranceStatus)
            .add("interestRate=" + interestRate)
            .add("legalDpd=" + legalDpd)
            .add("loanAmount='" + loanAmount + "'")
            .add("loanAnnuity='" + loanAnnuity + "'")
            .add("loanHealthInfo=" + loanHealthInfo)
            .add("loanName='" + loanName + "'")
            .add("loanTermInMonth=" + loanTermInMonth)
            .add("onSmp=" + onSmp)
            .add("paid='" + paid + "'")
            .add("paidInterest='" + paidInterest + "'")
            .add("paidPenalty='" + paidPenalty + "'")
            .add("paidPrincipal='" + paidPrincipal + "'")
            .add("paymentStatus=" + paymentStatus)
            .add("purchasePrice='" + purchasePrice + "'")
            .add("rating=" + rating)
            .add("remainingMonths=" + remainingMonths)
            .add("remainingPrincipal='" + remainingPrincipal + "'")
            .add("revenueRate=" + revenueRate)
            .add("smpFee='" + smpFee + "'")
            .add("smpPrice='" + smpPrice + "'")
            .add("smpSoldFor='" + smpSoldFor + "'")
            .add("toPay='" + toPay + "'")
            .toString();
    }
}
