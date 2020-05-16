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

package com.github.robozonky.strategy.natural;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.OptionalInt;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;

public interface Wrapper<T> {

    static Wrapper<LoanDescriptor> wrap(final LoanDescriptor descriptor, final PortfolioOverview portfolioOverview) {
        return new LoanWrapper(descriptor, portfolioOverview);
    }

    static Wrapper<InvestmentDescriptor> wrap(final InvestmentDescriptor descriptor,
            final PortfolioOverview portfolioOverview) {
        return new InvestmentWrapper(descriptor, portfolioOverview);
    }

    static Wrapper<ParticipationDescriptor> wrap(final ParticipationDescriptor descriptor,
            final PortfolioOverview portfolioOverview) {
        return new ParticipationWrapper(descriptor, portfolioOverview);
    }

    static Wrapper<ReservationDescriptor> wrap(final ReservationDescriptor descriptor,
            final PortfolioOverview portfolioOverview) {
        return new ReservationWrapper(descriptor, portfolioOverview);
    }

    long getId();

    boolean isInsuranceActive();

    Region getRegion();

    String getStory();

    MainIncomeType getMainIncomeType();

    Ratio getInterestRate();

    Ratio getRevenueRate();

    Purpose getPurpose();

    Rating getRating();

    int getOriginalTermInMonths();

    int getRemainingTermInMonths();

    int getOriginalAmount();

    int getOriginalAnnuity();

    OptionalInt getCurrentDpd();

    OptionalInt getMaxDpd();

    OptionalInt getDaysSinceDpd();

    BigDecimal getRemainingPrincipal();

    Optional<BigDecimal> getReturns();

    Optional<LoanHealth> getHealth();

    Optional<BigDecimal> getOriginalPurchasePrice();

    Optional<BigDecimal> getPrice();

    Optional<BigDecimal> getSellFee();

    Optional<BigDecimal> getDiscount();

    T getOriginal();

}
