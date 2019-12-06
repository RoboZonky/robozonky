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

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.StringJoiner;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.InsuranceStatus;
import com.github.robozonky.api.remote.enums.InvestmentType;
import com.github.robozonky.api.remote.enums.LoanHealthInfo;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.test.DateUtil;
import io.vavr.Lazy;

public class Investment extends BaseInvestment {

    @XmlElement
    private PaymentStatus paymentStatus;
    @XmlElement
    private LoanHealthInfo loanHealthInfo;
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
    private String loanName;
    private String nickname;
    private InsuranceStatus insuranceStatus = InsuranceStatus.NOT_INSURED;
    @XmlElement
    private OffsetDateTime investmentDate = DateUtil.offsetNow();
    private final Lazy<OffsetDateTime> actualInvestmentDate = Lazy.of(() -> {
        final int monthsElapsed = getLoanTermInMonth() - getRemainingMonths();
        final OffsetDateTime d = DateUtil.offsetNow().minusMonths(monthsElapsed);
        logger.debug("Investment date for investment #{} guessed to be {}.", getId(), d);
        return d;
    });
    @XmlElement
    private OffsetDateTime nextPaymentDate = investmentDate.plusMonths(1);
    @XmlElement
    private OffsetDateTime activeFrom;
    private OffsetDateTime activeTo;
    @XmlElement
    private OffsetDateTime smpFeeExpirationDate;
    private Ratio interestRate;
    @XmlElement
    private Ratio revenueRate;
    private Rating rating;
    private InvestmentType investmentType;

    // string-based money
    @XmlElement
    private String loanAnnuity = "0";
    private final Lazy<Money> moneyLoanAnnuity = Lazy.of(() -> Money.from(loanAnnuity));
    @XmlElement
    private String loanAmount = "0";
    private final Lazy<Money> moneyLoanAmount = Lazy.of(() -> Money.from(loanAmount));
    @XmlElement
    private String paid = "0";
    private final Lazy<Money> moneyPaid = Lazy.of(() -> Money.from(paid));
    @XmlElement
    private String toPay = "0";
    private final Lazy<Money> moneyToPay = Lazy.of(() -> Money.from(toPay));
    @XmlElement
    private String amountDue = "0";
    private final Lazy<Money> moneyAmountDue = Lazy.of(() -> Money.from(amountDue));
    @XmlElement
    private String paidInterest = "0";
    private final Lazy<Money> moneyPaidInterest = Lazy.of(() -> Money.from(paidInterest));
    @XmlElement
    private String dueInterest = "0";
    private final Lazy<Money> moneyDueInterest = Lazy.of(() -> Money.from(dueInterest));
    @XmlElement
    private String paidPrincipal = "0";
    private final Lazy<Money> moneyPaidPrincipal = Lazy.of(() -> Money.from(paidPrincipal));
    @XmlElement
    private String duePrincipal = "0";
    private final Lazy<Money> moneyDuePrincipal = Lazy.of(() -> Money.from(duePrincipal));
    @XmlElement
    private String expectedInterest = "0";
    private final Lazy<Money> moneyExpectedInterest = Lazy.of(() -> Money.from(expectedInterest));
    @XmlElement
    private String purchasePrice = "0";
    private final Lazy<Money> moneyPurchasePrice = Lazy.of(() -> Money.from(purchasePrice));
    @XmlElement
    private String remainingPrincipal = "0";
    private final Lazy<Money> moneyRemainingPrincipal = Lazy.of(() -> Money.from(remainingPrincipal));
    @XmlElement
    private String smpPrice = "0";
    private final Lazy<Money> moneySmpPrice = Lazy.of(() -> Money.from(smpPrice));
    @XmlElement
    private String smpSoldFor = "0";
    private final Lazy<Money> moneySmpSoldFor = Lazy.of(() -> Money.from(smpSoldFor));
    @XmlElement
    private String smpFee = "0";
    private final Lazy<Money> moneySmpFee = Lazy.of(() -> Money.from(smpFee));
    @XmlElement
    private String paidPenalty = "0";
    private final Lazy<Money> moneyPaidPenalty = Lazy.of(() -> Money.from(paidPenalty));

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

    Investment() {
        // for JAXB
    }

    public Investment(final Loan loan, final Money amount) {
        super(loan, amount);
        this.rating = loan.getRating();
        this.interestRate = rating.getInterestRate();
        this.revenueRate = rating.getMinimalRevenueRate(Instant.now());
        this.remainingPrincipal = amount.getValue().toPlainString();
        this.remainingMonths = loan.getTermInMonths();
        this.loanTermInMonth = loan.getTermInMonths();
        this.insuranceActive = loan.isInsuranceActive();
        this.additionallyInsured = loan.isAdditionallyInsured();
    }

    @XmlElement
    public Rating getRating() {
        return rating;
    }

    /**
     *
     * @return Empty when no longer relevant, such as when sold.
     */
    @XmlElement
    public Optional<LoanHealthInfo> getLoanHealthInfo() {
        return Optional.ofNullable(loanHealthInfo);
    }

    @XmlElement
    public Integer getLegalDpd() {
        return legalDpd;
    }

    @XmlElement
    public int getLoanInvestmentsCount() {
        return loanInvestmentsCount;
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
    public long getBorrowerNo() {
        return borrowerNo;
    }

    @XmlElement
    public String getLoanName() {
        return loanName;
    }

    @XmlElement
    public String getNickname() {
        return nickname;
    }

    public Optional<PaymentStatus> getPaymentStatus() {
        return Optional.ofNullable(paymentStatus);
    }

    /**
     *
     * @return This appears to always be null, so we guess from other fields.
     */
    public OffsetDateTime getInvestmentDate() {
        return Optional.ofNullable(investmentDate).orElseGet(actualInvestmentDate::get);
    }

    /**
     * In case of a presently delinquent loan, this always shows the date of the least recent instalment that is
     * delinquent.
     * @return Empty for loans where no payments are expected anymore.
     */
    public Optional<OffsetDateTime> getNextPaymentDate() {
        return Optional.ofNullable(nextPaymentDate);
    }

    /**
     *
     * @return If bought on SMP, then the timestamp of purchase. If invested from primary marketplace, then timestamp of
     * settlement (= empty when not yet settled).
     */
    @XmlElement
    public Optional<OffsetDateTime> getActiveFrom() {
        return Optional.ofNullable(activeFrom);
    }

    @XmlElement
    public OffsetDateTime getActiveTo() {
        return activeTo;
    }

    public Optional<OffsetDateTime> getSmpFeeExpirationDate() {
        return Optional.ofNullable(smpFeeExpirationDate);
    }

    @XmlElement
    public Ratio getInterestRate() {
        return interestRate;
    }

    @XmlElement
    public InsuranceStatus getInsuranceStatus() {
        return insuranceStatus;
    }

    /**
     * Semantics is identical to {@link BaseLoan#isInsuranceActive()} ()}.
     * @return
     */
    @XmlElement
    public boolean isInsuranceActive() {
        return insuranceActive;
    }

    /**
     * Semantics is identical to {@link BaseLoan#isAdditionallyInsured()}.
     * @return
     */
    @XmlElement
    public boolean isAdditionallyInsured() {
        return additionallyInsured;
    }

    @XmlElement
    public boolean isInstalmentPostponement() {
        return instalmentPostponement;
    }

    @XmlElement
    public boolean hasCollectionHistory() {
        return hasCollectionHistory;
    }

    public Optional<Ratio> getRevenueRate() {
        return Optional.ofNullable(revenueRate);
    }

    @XmlElement
    public InvestmentType getInvestmentType() {
        return investmentType;
    }

    // money types are all transient


    @XmlTransient
    public Money getLoanAnnuity() {
        return moneyLoanAnnuity.get();
    }

    @XmlTransient
    public Money getLoanAmount() {
        return moneyLoanAmount.get();
    }

    @XmlTransient
    public Money getPurchasePrice() {
        return moneyPurchasePrice.get();
    }

    @XmlTransient
    public Money getPaid() {
        return moneyPaid.get();
    }

    @XmlTransient
    public Money getToPay() {
        return moneyToPay.get();
    }

    @XmlTransient
    public Money getAmountDue() {
        return moneyAmountDue.get();
    }

    @XmlTransient
    public Money getPaidInterest() {
        return moneyPaidInterest.get();
    }

    @XmlTransient
    public Money getDueInterest() {
        return moneyDueInterest.get();
    }

    @XmlTransient
    public Money getPaidPrincipal() {
        return moneyPaidPrincipal.get();
    }

    @XmlTransient
    public Money getDuePrincipal() {
        return moneyDuePrincipal.get();
    }

    @XmlTransient
    public Money getExpectedInterest() {
        return moneyExpectedInterest.get();
    }

    /**
     * @return Empty when the investment is already sold.
     */
    @XmlTransient
    public Optional<Money> getRemainingPrincipal() {
        return Optional.ofNullable(remainingPrincipal)
                .map(principal -> moneyRemainingPrincipal.getOrElse((Money) null));
    }

    @XmlTransient
    public Optional<Money> getSmpSoldFor() {
        return Optional.ofNullable(smpSoldFor).map(soldFor -> moneySmpSoldFor.getOrElse((Money) null));
    }

    @XmlTransient
    public Money getPaidPenalty() {
        return moneyPaidPenalty.get();
    }

    @XmlTransient
    public Optional<Money> getSmpFee() {
        return Optional.ofNullable(smpFee).map(fee -> moneySmpFee.getOrElse((Money) null));
    }

    /**
     *
     * @return Empty when cannot be sold, that is when sold already.
     */
    @XmlTransient
    public Optional<Money> getSmpPrice() {
        return Optional.ofNullable(smpPrice).map(price -> moneySmpPrice.getOrElse((Money) null));
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", Investment.class.getSimpleName() + "[", "]")
                .add("super=" + super.toString())
                .add("activeFrom=" + activeFrom)
                .add("activeTo=" + activeTo)
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
                .add("investmentDate=" + investmentDate)
                .add("investmentType=" + investmentType)
                .add("inWithdrawal=" + inWithdrawal)
                .add("legalDpd=" + legalDpd)
                .add("loanAmount='" + loanAmount + "'")
                .add("loanAnnuity='" + loanAnnuity + "'")
                .add("loanHealthInfo=" + loanHealthInfo)
                .add("loanInvestmentsCount=" + loanInvestmentsCount)
                .add("loanName='" + loanName + "'")
                .add("loanTermInMonth=" + loanTermInMonth)
                .add("nextPaymentDate=" + nextPaymentDate)
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
                .add("smpFeeExpirationDate=" + smpFeeExpirationDate)
                .add("smpPrice='" + smpPrice + "'")
                .add("smpRelated=" + smpRelated)
                .add("smpSoldFor='" + smpSoldFor + "'")
                .add("toPay='" + toPay + "'")
                .toString();
    }
}
