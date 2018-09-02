/*
 * Copyright 2018 The RoboZonky Project
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
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import com.github.robozonky.api.remote.entities.InsurancePolicyPeriod;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

/**
 * This class is an adapted version of {@link RawInvestment}, with some computed fields added and others removed. Most
 * notably, {@link RawInvestment#getLoanName()} (and other similar fields) are removed as they are duplicates of the
 * same information on the loan and we therefore save memory by not including them.
 */
public interface Investment {

    /**
     * Create {@link Investment} based on {@link RawInvestment}, figuring out the investment date in case
     * {@link RawInvestment#getInvestmentDate()} is null.
     * @param investment Investment to sanitize.
     * @param investmentDateSupplier Date to assign in case the original is null.
     * @return Sanitized investment.
     */
    static Investment sanitized(final RawInvestment investment,
                                final Function<Investment, LocalDate> investmentDateSupplier) {
        return sanitize(investment, investmentDateSupplier).build();
    }

    static InvestmentBuilder custom() {
        return new MutableInvestmentImpl();
    }

    /**
     * Create modifiable {@link Investment} based on {@link RawInvestment}, figuring out the investment date in case
     * {@link RawInvestment#getInvestmentDate()} is null.
     * @param investment Investment to sanitize.
     * @param investmentDateSupplier Date to assign in case the original is null.
     * @return Sanitized modifiable investment.
     */
    static InvestmentBuilder sanitize(final RawInvestment investment,
                                      final Function<Investment, LocalDate> investmentDateSupplier) {
        return new MutableInvestmentImpl(investment, investmentDateSupplier);
    }

    static InvestmentBuilder fresh(final MarketplaceLoan loan, final int investedAmount) {
        return fresh(loan, BigDecimal.valueOf(investedAmount));
    }

    static InvestmentBuilder fresh(final MarketplaceLoan loan, final BigDecimal investedAmount) {
        return new MutableInvestmentImpl(loan, investedAmount);
    }

    static Investment fresh(final Participation participation, final Loan loan, final BigDecimal amount) {
        return Investment.fresh(loan, amount)
                .setId(participation.getInvestmentId())
                .setRemainingMonths(participation.getRemainingInstalmentCount())
                .setInvestmentDate(OffsetDateTime.now())
                .build();
    }

    int getLoanId();

    BigDecimal getOriginalPrincipal();

    int getId();

    BigDecimal getInterestRate();

    Rating getRating();

    /**
     * The original term.
     * @return
     */
    int getOriginalTerm();

    BigDecimal getPaidPrincipal();

    BigDecimal getDuePrincipal();

    BigDecimal getPaidInterest();

    BigDecimal getDueInterest();

    BigDecimal getExpectedInterest();

    BigDecimal getPaidPenalty();

    /**
     * How many monthly payments were expected to be made. May be less than {@link #getOriginalTerm()} in case of early
     * payments.
     * @return
     */
    int getCurrentTerm();

    /**
     * How many monthly payments are now remaining. Also see {@link #getCurrentTerm()}.
     * @return
     */
    int getRemainingMonths();

    OffsetDateTime getInvestmentDate();

    int getDaysPastDue();

    /**
     * Only available from Zonky when the investment is not yet sold. Otherwise we calculate from other fields.
     * @return
     */
    BigDecimal getRemainingPrincipal();

    /**
     * Only available from Zonky whew the loan either is present on the secondary marketplace, is ready to be sent there
     * or was already sold.
     * @return
     */
    Optional<BigDecimal> getSmpFee();

    /**
     * In case of a presently delinquent loan, this always shows the date of the least recent instalment that is
     * delinquent.
     * @return
     */
    Optional<OffsetDateTime> getNextPaymentDate();

    Optional<BigDecimal> getSmpSoldFor();

    InvestmentStatus getStatus();

    /**
     * @return Empty if sold.
     */
    Optional<PaymentStatus> getPaymentStatus();

    /**
     * @return Empty if sold.
     */
    Optional<Boolean> isInWithdrawal();

    boolean isOnSmp();

    boolean canBeOffered();

    boolean isInsuranceActive();

    boolean areInstalmentsPostponed();

    Collection<InsurancePolicyPeriod> getInsuranceHistory();
}
