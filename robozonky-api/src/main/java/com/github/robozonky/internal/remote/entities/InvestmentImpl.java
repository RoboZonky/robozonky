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
import java.util.StringJoiner;

import javax.xml.bind.annotation.XmlElement;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.InsuranceStatus;
import com.github.robozonky.api.remote.enums.InvestmentType;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.test.DateUtil;

public class InvestmentImpl extends BaseInvestmentImpl implements Investment {

    private static final Logger LOGGER = LogManager.getLogger(InvestmentImpl.class);

    @XmlElement
    private PaymentStatus paymentStatus;
    @XmlElement
    private LoanHealth loanHealthInfo;
    @XmlElement
    private boolean smpRelated;
    @XmlElement
    private boolean onSmp;
    @XmlElement
    private boolean canBeOffered;
    @XmlElement
    private boolean inWithdrawal;
    @XmlElement
    private boolean hasCollectionHistory;
    @XmlElement
    private boolean insuranceActive;
    @XmlElement
    private boolean additionallyInsured;
    @XmlElement
    private boolean instalmentPostponement;
    @XmlElement
    private int legalDpd;
    @XmlElement
    private int loanInvestmentsCount = 0;
    @XmlElement
    private int loanTermInMonth = 84;
    @XmlElement
    private int currentTerm = 0;
    @XmlElement
    private int remainingMonths = loanTermInMonth - currentTerm;
    @XmlElement
    private long borrowerNo = 0;
    @XmlElement
    private long loanPublicIdentifier = 0;
    @XmlElement
    private String loanName;
    @XmlElement
    private String nickname;
    @XmlElement
    private InsuranceStatus insuranceStatus = InsuranceStatus.NOT_INSURED;
    @XmlElement
    private Ratio interestRate;
    @XmlElement
    private Ratio revenueRate;
    @XmlElement
    private Rating rating;
    @XmlElement
    private InvestmentType investmentType;

    // OffsetDateTime is expensive to parse, and Investments are on the hot path. Only do it when needed.
    @XmlElement
    private String investmentDate;
    @XmlElement
    private String nextPaymentDate;
    @XmlElement
    private String activeFrom;
    @XmlElement
    private String activeTo;
    @XmlElement
    private String smpFeeExpirationDate;

    @XmlElement
    private Money loanAnnuity = Money.ZERO;
    @XmlElement
    private Money loanAmount = Money.ZERO;
    @XmlElement
    private Money paid = Money.ZERO;
    @XmlElement
    private Money toPay = Money.ZERO;
    @XmlElement
    private Money amountDue = Money.ZERO;
    @XmlElement
    private Money paidInterest = Money.ZERO;
    @XmlElement
    private Money dueInterest = Money.ZERO;
    @XmlElement
    private Money paidPrincipal = Money.ZERO;
    @XmlElement
    private Money duePrincipal = Money.ZERO;
    @XmlElement
    private Money expectedInterest = Money.ZERO;
    @XmlElement
    private Money purchasePrice = Money.ZERO;
    @XmlElement
    private Money remainingPrincipal = Money.ZERO;
    @XmlElement
    private Money smpPrice = Money.ZERO;
    @XmlElement
    private Money smpSoldFor = Money.ZERO;
    @XmlElement
    private Money smpFee = Money.ZERO;
    @XmlElement
    private Money paidPenalty = Money.ZERO;

    /*
     * Don't waste time deserializing some types, as we're never going to use them. Yet we do not want these reported as
     * unknown fields by Jackson.
     */
    @XmlElement
    private Object insuranceHistory;
    @XmlElement
    private Object loanHealthStats;
    @XmlElement
    private Object firstName;
    @XmlElement
    private Object surname;

    InvestmentImpl() {
        // for JAXB
    }

    public InvestmentImpl(final Loan loan, final Money amount) {
        super(loan, amount);
        this.loanPublicIdentifier = loan.getPublicIdentifier();
        this.rating = loan.getRating();
        this.interestRate = rating.getInterestRate();
        this.revenueRate = rating.getMinimalRevenueRate(Instant.now());
        this.remainingPrincipal = amount;
        this.purchasePrice = remainingPrincipal;
        this.remainingMonths = loan.getTermInMonths();
        this.loanTermInMonth = loan.getTermInMonths();
        this.insuranceActive = loan.isInsuranceActive();
        this.additionallyInsured = loan.isAdditionallyInsured();
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
    public int getLegalDpd() {
        return legalDpd;
    }

    @Override
    public int getLoanInvestmentsCount() {
        return loanInvestmentsCount;
    }

    @Override
    public int getLoanTermInMonth() {
        return loanTermInMonth;
    }

    @Override
    public int getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public boolean isSmpRelated() {
        return smpRelated;
    }

    @Override
    public boolean isOnSmp() {
        return onSmp;
    }

    @Override
    public boolean isCanBeOffered() {
        return canBeOffered;
    }

    @Override
    public boolean isInWithdrawal() {
        return inWithdrawal;
    }

    @Override
    public int getRemainingMonths() {
        return remainingMonths;
    }

    @Override
    public long getBorrowerNo() {
        return borrowerNo;
    }

    @Override
    public long getLoanPublicIdentifier() {
        return loanPublicIdentifier;
    }

    @Override
    public String getLoanName() {
        return loanName;
    }

    @Override
    public String getNickname() {
        return nickname;
    }

    @Override
    public Optional<PaymentStatus> getPaymentStatus() {
        return Optional.ofNullable(paymentStatus);
    }

    @Override
    public OffsetDateTime getInvestmentDate() {
        return Optional.ofNullable(investmentDate)
            .map(OffsetDateTimeAdapter::fromString)
            .orElseGet(() -> {
                final int monthsElapsed = getLoanTermInMonth() - getRemainingMonths();
                final OffsetDateTime d = DateUtil.offsetNow()
                    .minusMonths(monthsElapsed);
                LOGGER.debug("Investment date for investment #{} guessed to be {}.", getId(), d);
                return d;
            });
    }

    @Override
    public Optional<OffsetDateTime> getNextPaymentDate() {
        return Optional.ofNullable(nextPaymentDate)
            .map(OffsetDateTimeAdapter::fromString);
    }

    @Override
    public Optional<OffsetDateTime> getActiveFrom() {
        return Optional.ofNullable(activeFrom)
            .map(OffsetDateTimeAdapter::fromString);
    }

    @Override
    public Optional<OffsetDateTime> getActiveTo() {
        return Optional.ofNullable(activeTo)
            .map(OffsetDateTimeAdapter::fromString);
    }

    @Override
    public Optional<OffsetDateTime> getSmpFeeExpirationDate() {
        return Optional.ofNullable(smpFeeExpirationDate)
            .map(OffsetDateTimeAdapter::fromString);
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
    public boolean isAdditionallyInsured() {
        return additionallyInsured;
    }

    @Override
    public boolean isInstalmentPostponement() {
        return instalmentPostponement;
    }

    @Override
    public boolean hasCollectionHistory() {
        return hasCollectionHistory;
    }

    @Override
    public InvestmentType getInvestmentType() {
        return investmentType;
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

    public void setSmpRelated(final boolean smpRelated) {
        this.smpRelated = smpRelated;
    }

    public void setOnSmp(final boolean onSmp) {
        this.onSmp = onSmp;
    }

    public void setCanBeOffered(final boolean canBeOffered) {
        this.canBeOffered = canBeOffered;
    }

    public void setInWithdrawal(final boolean inWithdrawal) {
        this.inWithdrawal = inWithdrawal;
    }

    public void setHasCollectionHistory(final boolean hasCollectionHistory) {
        this.hasCollectionHistory = hasCollectionHistory;
    }

    public void setInsuranceActive(final boolean insuranceActive) {
        this.insuranceActive = insuranceActive;
    }

    public void setAdditionallyInsured(final boolean additionallyInsured) {
        this.additionallyInsured = additionallyInsured;
    }

    public void setInstalmentPostponement(final boolean instalmentPostponement) {
        this.instalmentPostponement = instalmentPostponement;
    }

    public void setLegalDpd(final int legalDpd) {
        this.legalDpd = legalDpd;
    }

    public void setLoanInvestmentsCount(final int loanInvestmentsCount) {
        this.loanInvestmentsCount = loanInvestmentsCount;
    }

    public void setLoanTermInMonth(final int loanTermInMonth) {
        this.loanTermInMonth = loanTermInMonth;
    }

    public void setCurrentTerm(final int currentTerm) {
        this.currentTerm = currentTerm;
    }

    public void setRemainingMonths(final int remainingMonths) {
        this.remainingMonths = remainingMonths;
    }

    public void setBorrowerNo(final long borrowerNo) {
        this.borrowerNo = borrowerNo;
    }

    public void setLoanPublicIdentifier(final long loanPublicIdentifier) {
        this.loanPublicIdentifier = loanPublicIdentifier;
    }

    public void setLoanName(final String loanName) {
        this.loanName = loanName;
    }

    public void setNickname(final String nickname) {
        this.nickname = nickname;
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

    public void setInvestmentType(final InvestmentType investmentType) {
        this.investmentType = investmentType;
    }

    private static String toOptionalString(final OffsetDateTime dateTime) {
        return Optional.ofNullable(dateTime)
            .map(OffsetDateTime::toString)
            .orElse(null);
    }

    public void setInvestmentDate(final OffsetDateTime investmentDate) {
        this.investmentDate = toOptionalString(investmentDate);
    }

    public void setNextPaymentDate(final OffsetDateTime nextPaymentDate) {
        this.nextPaymentDate = toOptionalString(nextPaymentDate);
    }

    public void setActiveFrom(final OffsetDateTime activeFrom) {
        this.activeFrom = toOptionalString(activeFrom);
    }

    public void setActiveTo(final OffsetDateTime activeTo) {
        this.activeTo = toOptionalString(activeTo);
    }

    public void setSmpFeeExpirationDate(final OffsetDateTime smpFeeExpirationDate) {
        this.smpFeeExpirationDate = toOptionalString(smpFeeExpirationDate);
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
            .add("activeFrom='" + activeFrom + "'")
            .add("activeTo='" + activeTo + "'")
            .add("additionallyInsured=" + additionallyInsured)
            .add("amountDue='" + amountDue + "'")
            .add("borrowerNo=" + borrowerNo)
            .add("canBeOffered=" + canBeOffered)
            .add("currentTerm=" + currentTerm)
            .add("dueInterest='" + dueInterest + "'")
            .add("duePrincipal='" + duePrincipal + "'")
            .add("expectedInterest='" + expectedInterest + "'")
            .add("hasCollectionHistory=" + hasCollectionHistory)
            .add("instalmentPostponement=" + instalmentPostponement)
            .add("insuranceActive=" + insuranceActive)
            .add("insuranceStatus=" + insuranceStatus)
            .add("interestRate=" + interestRate)
            .add("investmentDate='" + investmentDate + "'")
            .add("investmentType=" + investmentType)
            .add("inWithdrawal=" + inWithdrawal)
            .add("legalDpd=" + legalDpd)
            .add("loanAmount='" + loanAmount + "'")
            .add("loanAnnuity='" + loanAnnuity + "'")
            .add("loanHealthInfo=" + loanHealthInfo)
            .add("loanInvestmentsCount=" + loanInvestmentsCount)
            .add("loanName='" + loanName + "'")
            .add("loanPublicIdentifier='" + loanPublicIdentifier + "'")
            .add("loanTermInMonth=" + loanTermInMonth)
            .add("nextPaymentDate='" + nextPaymentDate + "'")
            .add("nickname='" + nickname + "'")
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
            .add("smpFeeExpirationDate='" + smpFeeExpirationDate + "'")
            .add("smpPrice='" + smpPrice + "'")
            .add("smpRelated=" + smpRelated)
            .add("smpSoldFor='" + smpSoldFor + "'")
            .add("toPay='" + toPay + "'")
            .toString();
    }
}
