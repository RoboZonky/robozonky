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

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.*;
import com.github.robozonky.internal.test.DateUtil;
import io.vavr.Lazy;

import javax.xml.bind.annotation.XmlElement;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Currency;
import java.util.Optional;

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
    private int loanTermInMonth = 84;
    private int currentTerm = 0;
    private int remainingMonths = loanTermInMonth - currentTerm;
    private String loanName;
    private String nickname;
    private String firstName;
    private String surname;
    private InsuranceStatus insuranceStatus = InsuranceStatus.NOT_INSURED;
    @XmlElement
    private OffsetDateTime investmentDate = DateUtil.offsetNow();
    private Lazy<OffsetDateTime> actualInvestmentDate = Lazy.of(() -> {
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
    private BigDecimal paid;
    private BigDecimal toPay;
    private BigDecimal amountDue;
    private BigDecimal paidInterest = BigDecimal.ZERO;
    private BigDecimal dueInterest;
    private BigDecimal paidPrincipal;
    private BigDecimal duePrincipal;
    private BigDecimal expectedInterest;
    private BigDecimal purchasePrice;
    private BigDecimal remainingPrincipal;
    @XmlElement
    private BigDecimal smpSoldFor;
    @XmlElement
    private BigDecimal smpFee;
    @XmlElement
    private BigDecimal smpPrice;
    private BigDecimal paidPenalty = BigDecimal.ZERO;
    private Ratio interestRate;
    @XmlElement
    private Ratio revenueRate;
    private Rating rating;
    private InvestmentType investmentType;
    @XmlElement
    private Collection<InsurancePolicyPeriod> insuranceHistory;

    Investment() {
        // for JAXB
    }

    public Investment(final Loan loan, final BigDecimal amount, final Currency currency) {
        super(loan, amount, currency);
        this.rating = loan.getRating();
        this.remainingPrincipal = amount;
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

    public Optional<PaymentStatus> getPaymentStatus() {
        return Optional.ofNullable(paymentStatus);
    }

    /**
     *
     * @return This appears to always be null, so we guess from other fields.
     */
    public OffsetDateTime getInvestmentDate() {
        return actualInvestmentDate.get();
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
    public Optional<BigDecimal> getSmpSoldFor() {
        return Optional.ofNullable(smpSoldFor);
    }

    @XmlElement
    public BigDecimal getPaidPenalty() {
        return paidPenalty;
    }

    @XmlElement
    public Optional<BigDecimal> getSmpFee() {
        return Optional.ofNullable(smpFee);
    }

    /**
     *
     * @return Empty when cannot be sold, that is when sold already.
     */
    public Optional<BigDecimal> getSmpPrice() {
        return Optional.ofNullable(smpPrice);
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

    public Collection<InsurancePolicyPeriod> getInsuranceHistory() {
        return insuranceHistory == null ? Collections.emptySet() : Collections.unmodifiableCollection(insuranceHistory);
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
}
