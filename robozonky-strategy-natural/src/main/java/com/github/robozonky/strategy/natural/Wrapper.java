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

package com.github.robozonky.strategy.natural;

import java.math.BigDecimal;

import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;

public interface Wrapper<T> {

    static Wrapper<LoanDescriptor> wrap(final LoanDescriptor descriptor) {
        return new LoanWrapper(descriptor);
    }

    static Wrapper<InvestmentDescriptor> wrap(final InvestmentDescriptor descriptor) {
        return new InvestmentWrapper(descriptor);
    }

    static Wrapper<ParticipationDescriptor> wrap(final ParticipationDescriptor descriptor) {
        return new ParticipationWrapper(descriptor);
    }

    boolean isInsuranceActive();

    Region getRegion();

    String getStory();

    MainIncomeType getMainIncomeType();

    BigDecimal getInterestRate();

    Purpose getPurpose();

    Rating getRating();

    int getOriginalTermInMonths();

    int getRemainingTermInMonths();

    int getOriginalAmount();

    BigDecimal getRemainingPrincipal();

    T getOriginal();

}
