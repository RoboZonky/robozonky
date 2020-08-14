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
import com.github.robozonky.api.remote.enums.MainIncomeIndustry;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public interface BaseLoan {

    MainIncomeType getMainIncomeType();

    MainIncomeIndustry getMainIncomeIndustry();

    Region getRegion();

    Purpose getPurpose();

    int getId();

    String getName();

    String getStory();

    int getTermInMonths();

    Ratio getInterestRate();

    Rating getRating();

    /**
     * @return True if the loan is insured at this very moment. Uninsured loans will have it return false.
     */
    boolean isInsuranceActive();

    Optional<Ratio> getRevenueRate();

    OffsetDateTime getDatePublished();

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
