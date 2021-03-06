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
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;

import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.Descriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.test.DateUtil;

abstract class AbstractWrapper<T extends Descriptor<?>> implements Wrapper<T> {

    private final T original;
    private final PortfolioOverview portfolioOverview;

    protected AbstractWrapper(final T original, final PortfolioOverview portfolioOverview) {
        this.original = original;
        this.portfolioOverview = portfolioOverview;
    }

    @Override
    public T getOriginal() {
        return original;
    }

    @Override
    public Optional<BigDecimal> getReturns() {
        return Optional.empty();
    }

    @Override
    public Optional<BigDecimal> getSellFee() {
        return Optional.empty();
    }

    @Override
    public Optional<LoanHealth> getHealth() {
        return Optional.empty();
    }

    @Override
    public Optional<BigDecimal> getOriginalPurchasePrice() {
        return Optional.empty();
    }

    @Override
    public Ratio getRevenueRate() { // Estimate; loans with ID < 400k are assumed to have the old pre-2019 fees.
        var feeDate = getLoanId() < 400_000 ? LocalDate.of(2019, 2, 28)
            .atStartOfDay()
            .atZone(Defaults.ZONKYCZ_ZONE_ID) : DateUtil.zonedNow();
        return Rating.forInterestRate(getInterestRate())
            .getMaximalRevenueRate(feeDate, portfolioOverview.getInvested());
    }

    @Override
    public Optional<BigDecimal> getSellPrice() {
        return Optional.empty();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final AbstractWrapper<T> that = (AbstractWrapper<T>) o;
        return Objects.equals(original, that.original);
    }

    @Override
    public int hashCode() {
        return Objects.hash(original);
    }
}
