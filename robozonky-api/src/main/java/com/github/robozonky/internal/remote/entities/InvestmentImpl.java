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
import javax.xml.bind.annotation.XmlTransient;

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
    private boolean smpRelated;
    private boolean onSmp;
    private boolean canBeOffered;
    private boolean inWithdrawal;
    private boolean hasCollectionHistory;
    private boolean insuranceActive;
    private boolean additionallyInsured;
    private boolean instalmentPostponement;
    private int legalDpd;
    private int loanInvestmentsCount = 0;
    private int loanTermInMonth = 84;
    private int currentTerm = 0;
    private int remainingMonths = loanTermInMonth - currentTerm;
    private long borrowerNo = 0;
    private long loanPublicIdentifier = 0;
    private String loanName;
    private String nickname;
    private InsuranceStatus insuranceStatus = InsuranceStatus.NOT_INSURED;
    private Ratio interestRate;
    private Ratio revenueRate;
    private Rating rating;
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
    @XmlElement
    public Rating getRating() {
        return rating;
    }

    @Override
    @XmlElement
    public Optional<LoanHealth> getLoanHealthInfo() {
        return Optional.ofNullable(loanHealthInfo);
    }

    @Override
    @XmlElement
    public int getLegalDpd() {
        return legalDpd;
    }

    @Override
    @XmlElement
    public int getLoanInvestmentsCount() {
        return loanInvestmentsCount;
    }

    @Override
    @XmlElement
    public int getLoanTermInMonth() {
        return loanTermInMonth;
    }

    @Override
    @XmlElement
    public int getCurrentTerm() {
        return currentTerm;
    }

    @Override
    @XmlElement
    public boolean isSmpRelated() {
        return smpRelated;
    }

    @Override
    @XmlElement
    public boolean isOnSmp() {
        return onSmp;
    }

    @Override
    @XmlElement
    public boolean isCanBeOffered() {
        return canBeOffered;
    }

    @Override
    @XmlElement
    public boolean isInWithdrawal() {
        return inWithdrawal;
    }

    @Override
    @XmlElement
    public int getRemainingMonths() {
        return remainingMonths;
    }

    @Override
    @XmlElement
    public long getBorrowerNo() {
        return borrowerNo;
    }

    @Override
    @XmlElement
    public long getLoanPublicIdentifier() {
        return loanPublicIdentifier;
    }

    @Override
    @XmlElement
    public String getLoanName() {
        return loanName;
    }

    @Override
    @XmlElement
    public String getNickname() {
        return nickname;
    }

    @Override
    public Optional<PaymentStatus> getPaymentStatus() {
        return Optional.ofNullable(paymentStatus);
    }

    @Override
    @XmlTransient
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
    @XmlTransient
    public Optional<OffsetDateTime> getNextPaymentDate() {
        return Optional.ofNullable(nextPaymentDate)
            .map(OffsetDateTimeAdapter::fromString);
    }

    @Override
    @XmlTransient
    public Optional<OffsetDateTime> getActiveFrom() {
        return Optional.ofNullable(activeFrom)
            .map(OffsetDateTimeAdapter::fromString);
    }

    @Override
    @XmlTransient
    public Optional<OffsetDateTime> getActiveTo() {
        return Optional.ofNullable(activeTo)
            .map(OffsetDateTimeAdapter::fromString);
    }

    @Override
    @XmlTransient
    public Optional<OffsetDateTime> getSmpFeeExpirationDate() {
        return Optional.ofNullable(smpFeeExpirationDate)
            .map(OffsetDateTimeAdapter::fromString);
    }

    @Override
    @XmlElement
    public Ratio getInterestRate() {
        return interestRate;
    }

    @Override
    @XmlElement
    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    @Override
    @XmlElement
    public InsuranceStatus getInsuranceStatus() {
        return insuranceStatus;
    }

    @Override
    @XmlElement
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    @Override
    @XmlElement
    public boolean isAdditionallyInsured() {
        return additionallyInsured;
    }

    @Override
    @XmlElement
    public boolean isInstalmentPostponement() {
        return instalmentPostponement;
    }

    @Override
    @XmlElement
    public boolean hasCollectionHistory() {
        return hasCollectionHistory;
    }

    @Override
    @XmlElement
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
    @XmlTransient
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
