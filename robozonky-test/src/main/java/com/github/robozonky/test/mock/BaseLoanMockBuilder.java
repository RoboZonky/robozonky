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

package com.github.robozonky.test.mock;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.BaseLoan;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.remote.enums.Region;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.mockito.Mockito.when;

abstract class BaseLoanMockBuilder<T extends BaseLoan, S extends BaseLoanMockBuilder<T, S>> extends BaseMockBuilder<T, S> {

    protected BaseLoanMockBuilder(Class<T> clz) {
        super(clz);
        when(mock.getId()).thenReturn(RANDOM.nextInt(Integer.MAX_VALUE));
    }

    public S setAmount(final int amount) {
        when(mock.getAmount()).thenReturn(Money.from(amount));
        return (S) this;
    }

    public S setRemainingInvestment(final int amount) {
        when(mock.getRemainingInvestment()).thenReturn(Money.from(amount));
        return (S) this;
    }

    public S setNonReservedRemainingInvestment(final int amount) {
        when(mock.getNonReservedRemainingInvestment()).thenReturn(Money.from(amount));
        return (S) this;
    }

    public S setRevenueRate(final Ratio rate) {
        when(mock.getRevenueRate()).thenReturn(Optional.ofNullable(rate));
        return (S) this;
    }

    public S setInterestRate(final Ratio rate) {
        when(mock.getInterestRate()).thenReturn(rate);
        return (S) this;
    }

    public S setAnnuity(final BigDecimal annuity) {
        when(mock.getAnnuity()).thenReturn(Money.from(annuity));
        return (S) this;
    }

    public S setRating(final Rating rating) {
        when(mock.getRating()).thenReturn(rating);
        when(mock.getInterestRate()).thenReturn(rating.getInterestRate());
        when(mock.getRevenueRate()).thenReturn(Optional.of(rating.getMaximalRevenueRate()));
        return (S) this;
    }

    public S setInsuranceActive(final boolean active) {
        when(mock.isInsuranceActive()).thenReturn(active);
        return (S) this;
    }

    public S setMainIncomeType(final MainIncomeType mainIncomeType) {
        when(mock.getMainIncomeType()).thenReturn(mainIncomeType);
        return (S) this;
    }

    public S setRegion(final Region region) {
        when(mock.getRegion()).thenReturn(region);
        return (S) this;
    }

    public S setPurpose(final Purpose purpose) {
        when(mock.getPurpose()).thenReturn(purpose);
        return (S) this;
    }

    public S setStory(final String story) {
        when(mock.getStory()).thenReturn(story);
        return (S) this;
    }

    public S setTermInMonths(final int termInMonths) {
        when(mock.getTermInMonths()).thenReturn(termInMonths);
        return (S) this;
    }

    public S setDatePublished(final OffsetDateTime datePublished) {
        when(mock.getDatePublished()).thenReturn(datePublished);
        return (S) this;
    }

    public S setName(final String name) {
        when(mock.getName()).thenReturn(name);
        return (S) this;
    }

}
