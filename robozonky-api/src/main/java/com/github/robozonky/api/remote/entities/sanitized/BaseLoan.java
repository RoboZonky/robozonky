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

public interface BaseLoan {

    MainIncomeType getMainIncomeType();

    BigDecimal getInvestmentRate();

    Region getRegion();

    Purpose getPurpose();

    int getId();

    String getName();

    String getStory();

    String getNickName();

    int getTermInMonths();

    BigDecimal getInterestRate();

    BigDecimal getRevenueRate();

    Rating getRating();

    boolean isTopped();

    int getAmount();

    /**
     * Does include the amount that is reserved. This means investing this amount may fail, due to part of it being
     * reserved.
     * @return
     */
    int getRemainingInvestment();

    BigDecimal getAnnuity();

    /**
     * This is the amount actually available for investment in the marketplace, as opposed to
     * {@link #getRemainingInvestment()}.
     * @return
     */
    int getNonReservedRemainingInvestment();

    boolean isCovered();

    boolean isPublished();

    OffsetDateTime getDatePublished();

    OffsetDateTime getDeadline();

    int getInvestmentsCount();

    int getActiveLoansCount();

    int getQuestionsCount();

    boolean isQuestionsAllowed();

    boolean isInsuranceActive();

    Collection<InsurancePolicyPeriod> getInsuranceHistory();

    int getUserId();

}
