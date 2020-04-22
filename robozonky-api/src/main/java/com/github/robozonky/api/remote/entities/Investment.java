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
import java.util.OptionalInt;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.InsuranceStatus;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

public interface Investment extends BaseInvestment {

    Rating getRating();

    /**
     * @return Empty when no longer relevant, such as when sold.
     */
    Optional<LoanHealth> getLoanHealthInfo();

    OptionalInt getLegalDpd();

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

    /**
     * /**
     * How many monthly payments are now remaining. Also see {@link #getCurrentTerm()}.
     *
     * @return
     */
    int getRemainingMonths();

    String getLoanName();

    Optional<PaymentStatus> getPaymentStatus();

    /**
     * @return This appears to always be null, so we guess from other fields.
     */
    OffsetDateTime getInvestmentDate();

    Ratio getInterestRate();

    Optional<Ratio> getRevenueRate();

    InsuranceStatus getInsuranceStatus();

    /**
     * Semantics is identical to {@link BaseLoan#isInsuranceActive()} ()}.
     *
     * @return
     */
    boolean isInsuranceActive();

    boolean isInstalmentPostponement();

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
