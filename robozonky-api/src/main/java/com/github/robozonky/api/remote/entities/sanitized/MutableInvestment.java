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

import com.github.robozonky.api.remote.entities.InsurancePolicyPeriod;
import com.github.robozonky.api.remote.enums.InvestmentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.Rating;

public interface MutableInvestment<T extends MutableInvestment<T>> extends Investment {

    T setLoanId(int loanId);

    T setAmountInvested(BigDecimal amountInvested);

    T setId(long id);

    T setInterestRate(BigDecimal interestRate);

    T setRating(Rating rating);

    T setOriginalTerm(int originalTerm);

    T setPaidPrincipal(BigDecimal paidPrincipal);

    T setDuePrincipal(BigDecimal duePrincipal);

    T setPaidInterest(BigDecimal paidInterest);

    T setDueInterest(BigDecimal dueInterest);

    T setExpectedInterest(BigDecimal expectedInterest);

    T setPaidPenalty(BigDecimal paidPenalty);

    T setCurrentTerm(int currentTerm);

    T setRemainingMonths(int remainingMonths);

    T setDaysPastDue(int daysPastDue);

    T setRemainingPrincipal(BigDecimal remainingPrincipal);

    T setSmpFee(BigDecimal smpFee);

    T setNextPaymentDate(OffsetDateTime nextPaymentDate);

    T setSmpSoldFor(BigDecimal smpSoldFor);

    T setOnSmp(boolean isOnSmp);

    T setOfferable(boolean canBeOffered);

    T setInvestmentDate(OffsetDateTime investmentDate);

    T setStatus(InvestmentStatus investmentStatus);

    T setPaymentStatus(PaymentStatus paymentStatus);

    T setInWithdrawal(boolean isInWithdrawal);

    T setInsuranceActive(boolean insuranceActive);

    T setInstalmentsPostponed(boolean instalmentsPostponed);

    T setInsuranceHistory(Collection<InsurancePolicyPeriod> insurancePolicyPeriods);

}
