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
import java.time.OffsetDateTime;
import java.util.Collection;

import com.github.robozonky.api.remote.entities.InsurancePolicyPeriod;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public interface MutableBaseLoan<T extends MutableBaseLoan<T>> extends BaseLoan {

    T setMainIncomeType(final MainIncomeType mainIncomeType);

    T setInvestmentRate(final BigDecimal investmentRate);

    T setRegion(final Region region);

    T setPurpose(final Purpose purpose);

    T setId(final int id);

    T setName(final String name);

    T setStory(final String story);

    T setNickName(final String nickName);

    T setTermInMonths(final int termInMonths);

    T setInterestRate(final BigDecimal interestRate);

    T setRevenueRate(final BigDecimal revenueRate);

    T setAnnuity(final BigDecimal annuity);

    T setRating(final Rating rating);

    T setTopped(final boolean isTopped);

    T setAmount(final int amount);

    T setRemainingInvestment(final int remainingInvestment);

    T setNonReservedRemainingInvestment(final int remainingInvestment);

    T setCovered(final boolean isCovered);

    T setPublished(final boolean isPublished);

    T setDatePublished(final OffsetDateTime datePublished);

    T setDeadline(final OffsetDateTime deadline);

    T setInvestmentsCount(final int investmentsCount);

    T setActiveLoansCount(final int activeLoansCount);

    T setQuestionsCount(final int questionsCount);

    T setQuestionsAllowed(final boolean isQuestionsAllowed);

    T setInsuranceActive(final boolean isInsuranceActive);

    T setInsuranceHistory(final Collection<InsurancePolicyPeriod> insuranceHistory);

    T setUserId(final int userId);

}
