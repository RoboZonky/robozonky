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

package com.github.robozonky.strategy.natural;

import java.math.BigDecimal;

import com.github.robozonky.api.remote.entities.sanitized.Reservation;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.ReservationDescriptor;

final class ReservationWrapper extends AbstractWrapper<ReservationDescriptor> {

    private final Reservation reservation;

    public ReservationWrapper(final ReservationDescriptor original) {
        super(original);
        this.reservation = original.item();
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
    public BigDecimal getInterestRate() {
        return reservation.getInterestRate();
    }

    @Override
    public BigDecimal getRevenueRate() {
        return reservation.getRevenueRate();
    }

    @Override
    public Purpose getPurpose() {
        return reservation.getPurpose();
    }

    @Override
    public Rating getRating() {
        return reservation.getRating();
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
        return reservation.getAmount();
    }

    @Override
    public int getOriginalAnnuity() {
        return reservation.getAnnuity().intValue();
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        throw new UnsupportedOperationException();
    }

    @Override
    public String toString() {
        return "Wrapper for reservation #" + reservation.getId();
    }

}
