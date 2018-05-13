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
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Optional;
import java.util.OptionalInt;

import com.github.robozonky.api.remote.entities.InsurancePolicyPeriod;
import com.github.robozonky.api.remote.entities.MyInvestment;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.RawInvestment;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

/**
 * This class is an adapted version of {@link RawInvestment}, with some computed fields added and others removed.
 */
public interface Investment {

    static Investment sanitized(final RawInvestment investment) {
        return sanitize(investment).build();
    }

    static InvestmentBuilder custom() {
        return new MutableInvestmentImpl();
    }

    static InvestmentBuilder sanitize(final RawInvestment investment) {
        return new MutableInvestmentImpl(investment);
    }

    static InvestmentBuilder fresh(final MarketplaceLoan loan, final int investedAmount) {
        return fresh(loan, BigDecimal.valueOf(investedAmount));
    }

    static InvestmentBuilder fresh(final MarketplaceLoan loan, final BigDecimal investedAmount) {
        return new MutableInvestmentImpl(loan, investedAmount);
    }

    static InvestmentBuilder fresh(final Loan loan, final int investedAmount) {
        return fresh(loan, BigDecimal.valueOf(investedAmount));
    }

    static InvestmentBuilder fresh(final Loan loan, final BigDecimal investedAmount) {
        return new MutableInvestmentImpl(loan, investedAmount);
    }

    static Investment fresh(final Participation participation, final Loan loan, final BigDecimal amount) {
        return Investment.fresh(loan, amount)
                .setId(participation.getInvestmentId())
                .setRemainingMonths(participation.getRemainingInstalmentCount())
                .build();
    }

    static void fillFrom(final Investment investment, final Loan loan) {
        if (investment instanceof MutableInvestment) {
            final MutableInvestment i = (MutableInvestment) investment;
            i.setInvestmentDate(loan.getMyInvestment().map(MyInvestment::getTimeCreated).orElse(null));
        } else {
            throw new IllegalArgumentException("Invalid investment " + investment);
        }
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
     * How many monthly payments were remaining to be made from {@link #getOriginalTerm()}.
     * May be less than {@link #getRemainingMonths()} in case of early payments.
     * @return
     */
    int getCurrentTerm();

    /**
     * How many monthly payments are now remaining. Also see {@link #getCurrentTerm()}.
     * @return
     */
    int getRemainingMonths();

    Optional<OffsetDateTime> getInvestmentDate();

    /**
     * Only available from Zonky when the investment is already funded, processed and not yet sold.
     * @return
     */
    OptionalInt getDaysPastDue();

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

    String getLoanName();

    String getNickname();

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
