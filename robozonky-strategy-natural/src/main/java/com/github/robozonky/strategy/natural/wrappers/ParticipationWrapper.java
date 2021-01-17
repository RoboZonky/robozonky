/*
 * Copyright 2021 The RoboZonky Project
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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Supplier;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.entities.ParticipationDetail;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class ParticipationWrapper extends AbstractWrapper<ParticipationDescriptor> {

    private final Participation participation;
    private final Supplier<ParticipationDetail> detail;

    public ParticipationWrapper(final ParticipationDescriptor descriptor, final PortfolioOverview portfolioOverview) {
        super(descriptor, portfolioOverview);
        this.participation = descriptor.item();
        this.detail = descriptor::detail;
    }

    @Override
    public long getId() {
        return participation.getId();
    }

    @Override
    public Region getRegion() {
        return detail.get()
            .getRegion();
    }

    @Override
    public String getStory() {
        return detail.get()
            .getStory();
    }

    @Override
    public long getLoanId() {
        return participation.getLoanId();
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
    public Purpose getPurpose() {
        return participation.getPurpose();
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
        return detail.get()
            .getAmount()
            .getValue()
            .intValue();
    }

    @Override
    public int getOriginalAnnuity() {
        return detail.get()
            .getAnnuity()
            .getValue()
            .intValue();
    }

    @Override
    public OptionalInt getCurrentDpd() {
        int currentDpd = detail.get()
            .getLoanHealthStats()
            .getCurrentDaysDue();
        return OptionalInt.of(currentDpd);
    }

    @Override
    public OptionalInt getLongestDpd() {
        int maxDpd = detail.get()
            .getLoanHealthStats()
            .getLongestDaysDue();
        return OptionalInt.of(maxDpd);
    }

    @Override
    public OptionalInt getDaysSinceDpd() {
        int daysSinceDpd = detail.get()
            .getLoanHealthStats()
            .getDaysSinceLastInDue();
        return OptionalInt.of(daysSinceDpd);
    }

    @Override
    public Optional<BigDecimal> getPrice() {
        return Optional.of(participation.getPrice()
            .getValue());
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        return participation.getRemainingPrincipal()
            .getValue();
    }

    @Override
    public Optional<LoanHealth> getHealth() {
        var currentDpd = getCurrentDpd().orElse(0);
        if (currentDpd == 0) {
            return Optional.of(LoanHealth.HEALTHY);
        }
        var longestDpd = getLongestDpd().orElse(0);
        if (longestDpd > 0) {
            return Optional.of(LoanHealth.HISTORICALLY_IN_DUE);
        }
        return Optional.of(LoanHealth.CURRENTLY_IN_DUE);
    }

    @Override
    public String toString() {
        return "Wrapper for loan #" + participation.getLoanId() + ", participation #" + getId();
    }
}
