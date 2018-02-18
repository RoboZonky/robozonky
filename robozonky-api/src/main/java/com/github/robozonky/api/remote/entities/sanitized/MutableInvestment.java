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

import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

public interface MutableInvestment<T extends MutableInvestment<T>> extends Investment {

    T setLoanId(final int loanId);

    T setAmountInvested(final BigDecimal amountInvested);

    T setId(final int id);

    T setLoanName(final String loanName);

    T setNickname(final String nickname);

    T setInterestRate(final BigDecimal interestRate);

    T setRating(final Rating rating);

    T setOriginalTerm(final int originalTerm);

    T setPaidPrincipal(final BigDecimal paidPrincipal);

    T setDuePrincipal(final BigDecimal duePrincipal);

    T setPaidInterest(final BigDecimal paidInterest);

    T setDueInterest(final BigDecimal dueInterest);

    T setExpectedInterest(final BigDecimal expectedInterest);

    T setPaidPenalty(final BigDecimal paidPenalty);

    T setCurrentTerm(final int currentTerm);

    T setRemainingMonths(final int remainingMonths);

    T setDaysPastDue(final int daysPastDue);

    T setRemainingPrincipal(final BigDecimal remainingPrincipal);

    T setSmpFee(final BigDecimal smpFee);

    T setNextPaymentDate(final OffsetDateTime nextPaymentDate);

    T setSmpSoldFor(final BigDecimal smpSoldFor);

    T setOnSmp(final boolean isOnSmp);

    T setOfferable(final boolean canBeOffered);

    T setInvestmentDate(final OffsetDateTime investmentDate);

    T setStatus(final InvestmentStatus investmentStatus);

    T setPaymentStatus(final PaymentStatus paymentStatus);

    T setInWithdrawal(final boolean isInWithdrawal);
}
