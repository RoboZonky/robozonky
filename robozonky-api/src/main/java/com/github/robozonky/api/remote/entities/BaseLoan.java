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
import java.util.Currency;
import java.util.Optional;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.Country;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public interface BaseLoan {

    Country getCountryOfOrigin();

    Currency getCurrency();

    MainIncomeType getMainIncomeType();

    Ratio getInvestmentRate();

    Region getRegion();

    Purpose getPurpose();

    int getId();

    long getPublicIdentifier();

    long getBorrowerNo();

    String getName();

    String getStory();

    String getNickName();

    int getTermInMonths();

    Ratio getInterestRate();

    Rating getRating();

    boolean isTopped();

    boolean isCovered();

    boolean isPublished();

    int getInvestmentsCount();

    int getActiveLoansCount();

    /**
     * @return True if the loan is insured at this very moment. Uninsured loans will have both this and
     *         {@link #isAdditionallyInsured()} return false.
     */
    boolean isInsuranceActive();

    /**
     * @return True if the loan will become insured at some later point in time. False when {@link #isInsuranceActive()}
     *         is true.
     */
    boolean isInsuredInFuture();

    /**
     * @return If the loan is insured, but did not start this way. Uninsured loans will have both this and
     *         {@link #isInsuranceActive()} return false.
     */
    boolean isAdditionallyInsured();

    int getUserId();

    Optional<Ratio> getRevenueRate();

    OffsetDateTime getDatePublished();

    OffsetDateTime getDeadline();

    Money getAmount();

    Money getRemainingInvestment();

    Money getNonReservedRemainingInvestment();

    Money getReservedAmount();

    Money getZonkyPlusAmount();

    Money getAnnuity();

    Money getPremium();

    /**
     * @return {@link #getAnnuity()} + {@link #getPremium()}
     */
    Money getAnnuityWithInsurance();
}
