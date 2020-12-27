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

package com.github.robozonky.strategy.natural.wrappers;

import java.math.BigDecimal;
import java.util.OptionalInt;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Reservation;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.ReservationDescriptor;

final class ReservationWrapper extends AbstractWrapper<ReservationDescriptor> {

    private final Reservation reservation;

    public ReservationWrapper(final ReservationDescriptor original, final PortfolioOverview portfolioOverview) {
        super(original, portfolioOverview);
        this.reservation = original.item();
    }

    @Override
    public long getId() {
        return reservation.getId();
    }

    @Override
    public boolean isInsuranceActive() {
        return reservation.isInsuranceActive();
    }

    @Override
    public Region getRegion() {
        return reservation.getRegion();
    }

    @Override
    public String getStory() {
        return reservation.getStory();
    }

    @Override
    public MainIncomeType getMainIncomeType() {
        return reservation.getMainIncomeType();
    }

    @Override
    public Ratio getInterestRate() {
        return reservation.getInterestRate();
    }

    @Override
    public Ratio getRevenueRate() {
        return reservation.getRevenueRate()
            .orElseGet(super::getRevenueRate);
    }

    @Override
    public Purpose getPurpose() {
        return reservation.getPurpose();
    }

    @Override
    public int getOriginalTermInMonths() {
        return reservation.getTermInMonths();
    }

    @Override
    public int getRemainingTermInMonths() {
        return reservation.getTermInMonths();
    }

    @Override
    public int getOriginalAmount() {
        return reservation.getAmount()
            .getValue()
            .intValue();
    }

    @Override
    public int getOriginalAnnuity() {
        return reservation.getAnnuity()
            .getValue()
            .intValue();
    }

    @Override
    public OptionalInt getCurrentDpd() {
        return OptionalInt.empty();
    }

    @Override
    public OptionalInt getLongestDpd() {
        return OptionalInt.empty();
    }

    @Override
    public OptionalInt getDaysSinceDpd() {
        return OptionalInt.empty();
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        return BigDecimal.valueOf(getOriginalAmount());
    }

    @Override
    public String toString() {
        return "Wrapper for reservation #" + getId();
    }
}
