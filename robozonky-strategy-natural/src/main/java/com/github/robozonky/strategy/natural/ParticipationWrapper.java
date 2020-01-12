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

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class ParticipationWrapper extends AbstractLoanWrapper<ParticipationDescriptor> {

    private final Participation participation;

    public ParticipationWrapper(final ParticipationDescriptor descriptor, final PortfolioOverview portfolioOverview) {
        super(descriptor, portfolioOverview);
        this.participation = descriptor.item();
    }

    @Override
    public long getId() {
        return participation.getId();
    }

    @Override
    public boolean isInsuranceActive() {
        return participation.isInsuranceActive();
    }

    @Override
    public MainIncomeType getMainIncomeType() {
        return participation.getIncomeType();
    }

    @Override
    public Ratio getInterestRate() {
        return participation.getInterestRate();
    }

    @Override
    public Ratio getRevenueRate() {
        return getLoan().getRevenueRate().orElseGet(this::estimateRevenueRate);
    }

    @Override
    public Purpose getPurpose() {
        return participation.getPurpose();
    }

    @Override
    public Rating getRating() {
        return participation.getRating();
    }

    @Override
    public int getOriginalTermInMonths() {
        return participation.getOriginalInstalmentCount();
    }

    @Override
    public int getRemainingTermInMonths() {
        return participation.getRemainingInstalmentCount();
    }

    @Override
    public int getOriginalAmount() {
        return getLoan().getAmount().getValue().intValue();
    }

    @Override
    public int getOriginalAnnuity() {
        return getLoan().getAnnuity().getValue().intValue();
    }

    @Override
    public Optional<BigDecimal> getPrice() {
        return Optional.of(participation.getPrice().getValue());
    }

    @Override
    public Optional<BigDecimal> getDiscount() {
        return Optional.of(participation.getDiscount().getValue());
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        return participation.getRemainingPrincipal().getValue();
    }

    @Override
    public String toString() {
        return "Wrapper for loan #" + participation.getLoanId() + ", participation #" + getId();
    }
}
