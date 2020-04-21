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

    // string-based money
    @XmlElement
    private String loanAnnuity = "0";
    @XmlElement
    private String loanAmount = "0";
    @XmlElement
    private String paid = "0";
    @XmlElement
    private String toPay = "0";
    @XmlElement
    private String amountDue = "0";
    @XmlElement
    private String paidInterest = "0";
    @XmlElement
    private String dueInterest = "0";
    @XmlElement
    private String paidPrincipal = "0";
    @XmlElement
    private String duePrincipal = "0";
    @XmlElement
    private String expectedInterest = "0";
    @XmlElement
    private String purchasePrice = "0";
    @XmlElement
    private String remainingPrincipal = "0";
    @XmlElement
    private String smpPrice = "0";
    @XmlElement
    private String smpSoldFor = "0";
    @XmlElement
    private String smpFee = "0";
    @XmlElement
    private String paidPenalty = "0";

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
        this.remainingPrincipal = amount.getValue()
            .toPlainString();
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

    // money types are all transient

    @Override
    @XmlTransient
    public Money getLoanAnnuity() {
        return Money.from(loanAnnuity);
    }

    @Override
    @XmlTransient
    public Money getLoanAmount() {
        return Money.from(loanAmount);
    }

    @Override
    @XmlTransient
    public Money getPurchasePrice() {
        return Money.from(purchasePrice);
    }

    @Override
    @XmlTransient
    public Money getPaid() {
        return Money.from(paid);
    }

    @Override
    @XmlTransient
    public Money getToPay() {
        return Money.from(toPay);
    }

    @Override
    @XmlTransient
    public Money getAmountDue() {
        return Money.from(amountDue);
    }

    @Override
    @XmlTransient
    public Money getPaidInterest() {
        return Money.from(paidInterest);
    }

    @Override
    @XmlTransient
    public Money getDueInterest() {
        return Money.from(dueInterest);
    }

    @Override
    @XmlTransient
    public Money getPaidPrincipal() {
        return Money.from(paidPrincipal);
    }

    @Override
    @XmlTransient
    public Money getDuePrincipal() {
        return Money.from(duePrincipal);
    }

    @Override
    @XmlTransient
    public Money getExpectedInterest() {
        return Money.from(expectedInterest);
    }

    @Override
    @XmlTransient
    public Optional<Money> getRemainingPrincipal() {
        return Optional.ofNullable(remainingPrincipal)
            .map(Money::from);
    }

    @Override
    @XmlTransient
    public Optional<Money> getSmpSoldFor() {
        return Optional.ofNullable(smpSoldFor)
            .map(Money::from);
    }

    @Override
    @XmlTransient
    public Money getPaidPenalty() {
        return Money.from(paidPenalty);
    }

    @Override
    @XmlTransient
    public Optional<Money> getSmpFee() {
        return Optional.ofNullable(smpFee)
            .map(Money::from);
    }

    @Override
    @XmlTransient
    public Optional<Money> getSmpPrice() {
        return Optional.ofNullable(smpPrice)
            .map(Money::from);
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
