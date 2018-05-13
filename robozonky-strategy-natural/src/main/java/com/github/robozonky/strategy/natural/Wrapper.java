/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.strategy.natural;

import java.math.BigDecimal;

import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

public interface Wrapper {

    boolean isInsuranceActive();

    int getLoanId();

    Region getRegion();

    String getStory();

    MainIncomeType getMainIncomeType();

    BigDecimal getInterestRate();

    Purpose getPurpose();

    Rating getRating();

    int getOriginalTermInMonths();

    int getRemainingTermInMonths();

    // FIXME needs to be BigDecimal
    int getOriginalAmount();

    BigDecimal getRemainingAmount();

    String getIdentifier();
}
