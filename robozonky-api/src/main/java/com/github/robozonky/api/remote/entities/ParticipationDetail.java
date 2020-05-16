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

import java.net.URL;
import java.time.OffsetDateTime;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.MainIncomeIndustry;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Region;

public interface ParticipationDetail {

    long getId();

    MainIncomeType getIncomeType();

    MainIncomeIndustry getMainIncomeIndustry();

    Ratio getInterestRate();

    Ratio getRevenueRate();

    String getName();

    boolean isInsuranceActive();

    String getStory();

    Purpose getPurpose();

    URL getUrl();

    Region getRegion();

    LoanHealthStats getLoanHealthStats();

    OffsetDateTime getNextPaymentDate();

    Money getAmount();

    Money getAnnuity();

    int getActiveLoansCount();

    int getDueInstalmentsCount();

}
