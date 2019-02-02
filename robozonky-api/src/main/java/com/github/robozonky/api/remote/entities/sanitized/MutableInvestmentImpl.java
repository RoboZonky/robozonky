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

package com.github.robozonky.api.remote.entities.sanitized;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.InsurancePolicyPeriod;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.api.Defaults;
import com.github.robozonky.internal.util.DateUtil;
import com.github.robozonky.internal.util.RandomUtil;
import com.github.robozonky.internal.util.ToStringBuilder;
import io.vavr.Lazy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class MutableInvestmentImpl implements InvestmentBuilder {

    private static final Logger LOGGER = LogManager.getLogger(MutableInvestmentImpl.class);
    private final AtomicReference<OffsetDateTime> investmentDate = new AtomicReference<>();
    private final Lazy<String> toString = Lazy.of(() -> ToStringBuilder.createFor(this, "toString"));
    private long id;
    private int loanId, currentTerm, originalTerm, remainingMonths;
    private Integer daysPastDue;
    private OffsetDateTime nextPaymentDate;
    private boolean canBeOffered, isOnSmp, isInsuranceActive, areInstalmentsPostponed;
    private Boolean isInWithdrawal;
    private BigDecimal originalPrincipal, interestRate, paidPrincipal, duePrincipal, paidInterest, dueInterest,
            expectedInterest, paidPenalty, remainingPrincipal, smpFee, smpSoldFor;
    private BigDecimal revenueRate;
    private Rating rating;
    private InvestmentStatus status;
    private PaymentStatus paymentStatus;
    private Collection<InsurancePolicyPeriod> insuranceHistory = Collections.emptyList();
    // default value for investment date, in case it is null
    private Supplier<LocalDate> investmentDateSupplier = () -> DateUtil.localNow().toLocalDate();

    MutableInvestmentImpl() {
        this.id = RandomUtil.getNextInt(); // simplifies tests which do not have to generate random IDs themselves
    }

    /**
     * Create a new instance, basing it on the original {@link RawInvestment}, which will lazy-load investment date
     * if necessary.
     * @param investment The original.
     * @param investmentDateSupplier Will use this to lazy-load if the original is null.
     */
    MutableInvestmentImpl(final RawInvestment investment,
                          final Function<Investment, LocalDate> investmentDateSupplier) {
        LOGGER.trace("Sanitizing investment #{} for loan #{}.", investment.getId(), investment.getLoanId());
        this.loanId = investment.getLoanId();
        this.id = investment.getId();
        this.currentTerm = investment.getCurrentTerm();
        this.originalTerm = investment.getLoanTermInMonth();
        this.remainingMonths = investment.getRemainingMonths();
        this.daysPastDue = investment.getLegalDpd();
        this.investmentDate.set(investment.getInvestmentDate());
        this.nextPaymentDate = investment.getNextPaymentDate();
        this.canBeOffered = investment.isCanBeOffered();
        this.isOnSmp = investment.isOnSmp();
        this.originalPrincipal = investment.getPurchasePrice();
        this.interestRate = investment.getInterestRate();
        this.revenueRate = investment.getRevenueRate();
        this.paidPrincipal = investment.getPaidPrincipal();
        this.duePrincipal = investment.getDuePrincipal();
        this.paidInterest = investment.getPaidInterest();
        this.dueInterest = investment.getDueInterest();
        this.expectedInterest = investment.getExpectedInterest();
        this.paidPenalty = investment.getPaidPenalty();
        this.remainingPrincipal = investment.getRemainingPrincipal();
        this.smpFee = investment.getSmpFee();
        this.smpSoldFor = investment.getSmpSoldFor();
        this.rating = investment.getRating();
        this.isInWithdrawal = investment.isInWithdrawal();
        this.status = investment.getStatus();
        this.paymentStatus = investment.getPaymentStatus();
        this.isInsuranceActive = investment.isInsuranceActive();
        this.areInstalmentsPostponed = investment.isInstalmentPostponement();
        setInsuranceHistory(investment.getInsuranceHistory());
        this.investmentDateSupplier = () -> investmentDateSupplier.apply(this);
    }

    MutableInvestmentImpl(final MarketplaceLoan loan, final BigDecimal originalPrincipal) {
        loan.getMyInvestment().ifPresent(i -> {
            this.id = i.getId();
            this.investmentDate.set(i.getTimeCreated());
        });
        this.loanId = loan.getId();
        this.currentTerm = loan.getTermInMonths();
        this.originalTerm = loan.getTermInMonths();
        this.remainingMonths = loan.getTermInMonths();
        this.daysPastDue = 0;
        this.canBeOffered = false;
        this.isOnSmp = false;
        this.originalPrincipal = originalPrincipal;
        this.interestRate = loan.getInterestRate();
        this.revenueRate = loan.getRevenueRate();
        this.paidPrincipal = BigDecimal.ZERO;
        this.duePrincipal = BigDecimal.ZERO;
        this.paidInterest = BigDecimal.ZERO;
        this.dueInterest = BigDecimal.ZERO;
        this.paidPenalty = BigDecimal.ZERO;
        this.remainingPrincipal = originalPrincipal;
        this.rating = loan.getRating();
        this.isInWithdrawal = false;
        this.status = InvestmentStatus.ACTIVE;
        this.paymentStatus = PaymentStatus.NOT_COVERED;
        this.isInsuranceActive = loan.isInsuranceActive();
        this.areInstalmentsPostponed = false;
        this.setInsuranceHistory(loan.getInsuranceHistory());
    }

    @Override
    public InvestmentBuilder setLoanId(final int loanId) {
        this.loanId = loanId;
        return this;
    }

    @Override
    public InvestmentBuilder setOriginalPrincipal(final BigDecimal amountInvested) {
        this.originalPrincipal = amountInvested;
        return this;
    }

    @Override
    public InvestmentBuilder setId(final long id) {
        this.id = id;
        return this;
    }

    @Override
    public InvestmentBuilder setInterestRate(final BigDecimal interestRate) {
        this.interestRate = interestRate;
        return this;
    }

    @Override
    public InvestmentBuilder setRevenueRate(final BigDecimal revenueRate) {
        this.revenueRate = revenueRate;
        return this;
    }

    @Override
    public InvestmentBuilder setRating(final Rating rating) {
        this.rating = rating;
        return this;
    }

    @Override
    public InvestmentBuilder setOriginalTerm(final int originalTerm) {
        this.originalTerm = originalTerm;
        return this;
    }

    @Override
    public InvestmentBuilder setPaidPrincipal(final BigDecimal paidPrincipal) {
        this.paidPrincipal = paidPrincipal;
        return this;
    }

    @Override
    public InvestmentBuilder setDuePrincipal(final BigDecimal duePrincipal) {
        this.duePrincipal = duePrincipal;
        return this;
    }

    @Override
    public InvestmentBuilder setPaidInterest(final BigDecimal paidInterest) {
        this.paidInterest = paidInterest;
        return this;
    }

    @Override
    public InvestmentBuilder setDueInterest(final BigDecimal dueInterest) {
        this.dueInterest = dueInterest;
        return this;
    }

    @Override
    public InvestmentBuilder setExpectedInterest(final BigDecimal expectedInterest) {
        this.expectedInterest = expectedInterest;
        return this;
    }

    @Override
    public InvestmentBuilder setPaidPenalty(final BigDecimal paidPenalty) {
        this.paidPenalty = paidPenalty;
        return this;
    }

    @Override
    public InvestmentBuilder setCurrentTerm(final int currentTerm) {
        this.currentTerm = currentTerm;
        return this;
    }

    @Override
    public InvestmentBuilder setRemainingMonths(final int remainingMonths) {
        this.remainingMonths = remainingMonths;
        return this;
    }

    @Override
    public InvestmentBuilder setDaysPastDue(final int daysPastDue) {
        this.daysPastDue = daysPastDue;
        return this;
    }

    @Override
    public InvestmentBuilder setRemainingPrincipal(final BigDecimal remainingPrincipal) {
        this.remainingPrincipal = remainingPrincipal;
        return this;
    }

    @Override
    public InvestmentBuilder setSmpFee(final BigDecimal smpFee) {
        this.smpFee = smpFee;
        return this;
    }

    @Override
    public InvestmentBuilder setNextPaymentDate(final OffsetDateTime nextPaymentDate) {
        this.nextPaymentDate = nextPaymentDate;
        return this;
    }

    @Override
    public InvestmentBuilder setSmpSoldFor(final BigDecimal smpSoldFor) {
        this.smpSoldFor = smpSoldFor;
        return this;
    }

    @Override
    public InvestmentBuilder setOnSmp(final boolean isOnSmp) {
        this.isOnSmp = isOnSmp;
        return this;
    }

    @Override
    public InvestmentBuilder setOfferable(final boolean canBeOffered) {
        this.canBeOffered = canBeOffered;
        return this;
    }

    @Override
    public synchronized InvestmentBuilder setInvestmentDate(final OffsetDateTime investmentDate) {
        this.investmentDate.set(investmentDate);
        return this;
    }

    @Override
    public InvestmentBuilder setStatus(final InvestmentStatus investmentStatus) {
        this.status = investmentStatus;
        return this;
    }

    @Override
    public InvestmentBuilder setPaymentStatus(final PaymentStatus paymentStatus) {
        this.paymentStatus = paymentStatus;
        return this;
    }

    @Override
    public InvestmentBuilder setInWithdrawal(final boolean isInWithdrawal) {
        this.isInWithdrawal = isInWithdrawal;
        return this;
    }

    @Override
    public InvestmentBuilder setInsuranceActive(final boolean insuranceActive) {
        this.isInsuranceActive = insuranceActive;
        return this;
    }

    @Override
    public InvestmentBuilder setInstalmentsPostponed(final boolean instalmentsPostponed) {
        this.areInstalmentsPostponed = instalmentsPostponed;
        return this;
    }

    @Override
    public InvestmentBuilder setInsuranceHistory(final Collection<InsurancePolicyPeriod> insurancePolicyPeriods) {
        final boolean isEmpty = insurancePolicyPeriods == null || insurancePolicyPeriods.isEmpty();
        this.insuranceHistory = isEmpty ? Collections.emptyList() : new ArrayList<>(insurancePolicyPeriods);
        return this;
    }

    @Override
    public int getLoanId() {
        return loanId;
    }

    @Override
    public long getId() {
        return id;
    }

    @Override
    public int getCurrentTerm() {
        return currentTerm;
    }

    @Override
    public int getOriginalTerm() {
        return originalTerm;
    }

    @Override
    public int getRemainingMonths() {
        return remainingMonths;
    }

    @Override
    public OffsetDateTime getInvestmentDate() {
        return investmentDate.updateAndGet(old -> {
            if (old == null) {
                return investmentDateSupplier.get().atStartOfDay(Defaults.ZONE_ID).toOffsetDateTime();
            }
            return old;
        });
    }

    @Override
    public int getDaysPastDue() {
        return daysPastDue == null ? 0 : daysPastDue;
    }

    @Override
    public Optional<OffsetDateTime> getNextPaymentDate() {
        return Optional.ofNullable(nextPaymentDate);
    }

    @Override
    public boolean canBeOffered() {
        return canBeOffered;
    }

    @Override
    public boolean isInsuranceActive() {
        return isInsuranceActive;
    }

    @Override
    public boolean areInstalmentsPostponed() {
        return areInstalmentsPostponed;
    }

    @Override
    public Collection<InsurancePolicyPeriod> getInsuranceHistory() {
        return Collections.unmodifiableCollection(insuranceHistory);
    }

    @Override
    public boolean isOnSmp() {
        return isOnSmp;
    }

    @Override
    public BigDecimal getOriginalPrincipal() {
        return originalPrincipal;
    }

    @Override
    public BigDecimal getInterestRate() {
        return interestRate;
    }

    @Override
    public BigDecimal getRevenueRate() {
        return revenueRate;
    }

    @Override
    public BigDecimal getPaidPrincipal() {
        return paidPrincipal;
    }

    @Override
    public BigDecimal getDuePrincipal() {
        return duePrincipal;
    }

    @Override
    public BigDecimal getPaidInterest() {
        return paidInterest;
    }

    @Override
    public BigDecimal getDueInterest() {
        return dueInterest;
    }

    @Override
    public BigDecimal getExpectedInterest() {
        return expectedInterest;
    }

    @Override
    public BigDecimal getPaidPenalty() {
        return paidPenalty;
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        if (remainingPrincipal != null) {
            return remainingPrincipal;
        } else {
            return originalPrincipal.subtract(paidPrincipal);
        }
    }

    @Override
    public Optional<BigDecimal> getSmpFee() {
        return Optional.ofNullable(smpFee);
    }

    @Override
    public Optional<BigDecimal> getSmpSoldFor() {
        return Optional.ofNullable(smpSoldFor);
    }

    @Override
    public InvestmentStatus getStatus() {
        return status;
    }

    @Override
    public Optional<PaymentStatus> getPaymentStatus() {
        return Optional.ofNullable(paymentStatus);
    }

    @Override
    public Optional<Boolean> isInWithdrawal() {
        return Optional.ofNullable(isInWithdrawal);
    }

    @Override
    public Rating getRating() {
        return rating;
    }

    @Override
    public final String toString() {
        return toString.get();
    }
}
