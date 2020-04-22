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

package com.github.robozonky.api.remote.entities;

import java.time.OffsetDateTime;
import java.util.Optional;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.InsuranceStatus;
import com.github.robozonky.api.remote.enums.InvestmentType;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

public interface Investment extends BaseInvestment {

    Rating getRating();

    /**
     * @return Empty when no longer relevant, such as when sold.
     */
    Optional<LoanHealth> getLoanHealthInfo();

    int getLegalDpd();

    int getLoanInvestmentsCount();

    /**
     * The original term.
     *
     * @return
     */
    int getLoanTermInMonth();

    /**
     * How many monthly payments were remaining to be made from {@link #getLoanTermInMonth()}.
     * May be less than {@link #getRemainingMonths()} in case of early payments.
     *
     * @return
     */
    int getCurrentTerm();

    boolean isOnSmp();

    boolean isCanBeOffered();

    /**
     * The client terminated the loan contract. The investment can therefore not be sold on secondary marketplace.
     *
     * @return
     */
    boolean isInWithdrawal();

    /**
     * How many monthly payments are now remaining. Also see {@link #getCurrentTerm()}.
     *
     * @return
     */
    int getRemainingMonths();

    long getBorrowerNo();

    long getLoanPublicIdentifier();

    String getLoanName();

    String getNickname();

    Optional<PaymentStatus> getPaymentStatus();

    /**
     * @return This appears to always be null, so we guess from other fields.
     */
    OffsetDateTime getInvestmentDate();

    /**
     * In case of a presently delinquent loan, this always shows the date of the least recent instalment that is
     * delinquent.
     *
     * @return Empty for loans where no payments are expected anymore.
     */
    Optional<OffsetDateTime> getNextPaymentDate();

    /**
     * @return If bought on SMP, then the timestamp of purchase. If invested from primary marketplace, then timestamp of
     *         settlement (= empty when not yet settled).
     */
    Optional<OffsetDateTime> getActiveFrom();

    Optional<OffsetDateTime> getActiveTo();

    Optional<OffsetDateTime> getSmpFeeExpirationDate();

    Ratio getInterestRate();

    Optional<Ratio> getRevenueRate();

    InsuranceStatus getInsuranceStatus();

    /**
     * Semantics is identical to {@link BaseLoan#isInsuranceActive()} ()}.
     *
     * @return
     */
    boolean isInsuranceActive();

    /**
     * Semantics is identical to {@link BaseLoan#isAdditionallyInsured()}.
     *
     * @return
     */
    boolean isAdditionallyInsured();

    boolean isInstalmentPostponement();

    boolean hasCollectionHistory();

    InvestmentType getInvestmentType();

    Money getLoanAnnuity();

    Money getLoanAmount();

    Money getPurchasePrice();

    Money getPaid();

    Money getToPay();

    Money getAmountDue();

    Money getPaidInterest();

    Money getDueInterest();

    Money getPaidPrincipal();

    Money getDuePrincipal();

    Money getExpectedInterest();

    /**
     * @return Empty when the investment is already sold.
     */
    Optional<Money> getRemainingPrincipal();

    Optional<Money> getSmpSoldFor();

    Money getPaidPenalty();

    Optional<Money> getSmpFee();

    /**
     * @return Empty when cannot be sold, that is when sold already.
     */
    Optional<Money> getSmpPrice();
}
