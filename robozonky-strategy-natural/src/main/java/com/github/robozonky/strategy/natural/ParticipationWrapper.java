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
import java.util.Objects;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;

public class ParticipationWrapper implements Wrapper {

    private final Participation participation;
    private final String identifier;

    public ParticipationWrapper(final Participation participation) {
        this.participation = participation;
        this.identifier = "Loan #" + participation.getLoanId() + " (participation #" + participation.getId() + ")";
    }

    public int getLoanId() {
        return participation.getLoanId();
    }

    public MainIncomeType getMainIncomeType() {
        return participation.getIncomeType();
    }

    @Override
    public BigDecimal getInterestRate() {
        return participation.getInterestRate();
    }

    public Purpose getPurpose() {
        return participation.getPurpose();
    }

    public Rating getRating() {
        return participation.getRating();
    }

    @Override
    public int getOriginalTermInMonths() {
        return participation.getOriginalInstalmentCount();
    }

    public int getRemainingTermInMonths() {
        return participation.getRemainingInstalmentCount();
    }

    public BigDecimal getRemainingAmount() {
        return participation.getRemainingPrincipal();
    }

    @Override
    public String getIdentifier() {
        return identifier;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final ParticipationWrapper wrapper = (ParticipationWrapper) o;
        return Objects.equals(identifier, wrapper.identifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(identifier);
    }
}
