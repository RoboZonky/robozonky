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

import java.time.Period;
import java.util.Optional;
import java.util.StringJoiner;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.strategies.ReservationMode;
import com.github.robozonky.internal.test.DateUtil;

class DefaultValues {

    private final DefaultPortfolio portfolio;
    private ReservationMode reservationMode = null;
    private SellingMode sellingMode = null;
    private Money targetPortfolioSize = Money.from(Long.MAX_VALUE);
    private MoneyRange investmentSize = new MoneyRange();
    private MoneyRange purchaseSize = new MoneyRange();
    private DefaultInvestmentShare investmentShare = new DefaultInvestmentShare();
    private ExitProperties exitProperties;

    public DefaultValues(final DefaultPortfolio portfolio) {
        this.portfolio = portfolio;
    }

    public DefaultPortfolio getPortfolio() {
        return portfolio;
    }

    public Optional<ReservationMode> getReservationMode() {
        return Optional.ofNullable(reservationMode);
    }

    public void setReservationMode(final ReservationMode reservationMode) {
        this.reservationMode = reservationMode;
    }

    public Optional<SellingMode> getSellingMode() {
        return Optional.ofNullable(sellingMode);
    }

    public void setSellingMode(final SellingMode sellingMode) {
        this.sellingMode = sellingMode;
    }

    public void setExitProperties(final ExitProperties properties) {
        this.exitProperties = properties;
    }

    public boolean isSelloffStarted() {
        if (exitProperties == null) {
            return false;
        } else {
            return exitProperties.getSelloffStart()
                .isBefore(DateUtil.zonedNow()
                    .toLocalDate());
        }
    }

    public long getMonthsBeforeExit() {
        if (exitProperties == null) {
            return -1;
        } else {
            return Math.max(0, Period.between(DateUtil.zonedNow()
                .toLocalDate(),
                    exitProperties.getAccountTermination())
                .toTotalMonths());
        }
    }

    public Money getTargetPortfolioSize() {
        return targetPortfolioSize;
    }

    public void setTargetPortfolioSize(final long targetPortfolioSize) {
        if (targetPortfolioSize <= 0) {
            throw new IllegalArgumentException("Target portfolio size must be a positive number.");
        }
        this.targetPortfolioSize = Money.from(targetPortfolioSize);
    }

    public DefaultInvestmentShare getInvestmentShare() {
        return investmentShare;
    }

    public void setInvestmentShare(final DefaultInvestmentShare investmentShare) {
        this.investmentShare = investmentShare;
    }

    public MoneyRange getInvestmentSize() {
        return investmentSize;
    }

    public void setInvestmentSize(final int investmentSize) {
        if (investmentSize % 200 != 0) {
            throw new IllegalArgumentException("Investment size must be divisible by 200: " + investmentSize);
        }
        this.investmentSize = new MoneyRange(investmentSize, investmentSize);
    }

    public MoneyRange getPurchaseSize() {
        return purchaseSize;
    }

    public void setPurchaseSize(final int purchaseSize) {
        this.purchaseSize = new MoneyRange(1, purchaseSize);
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", DefaultValues.class.getSimpleName() + "[", "]")
            .add("portfolio=" + portfolio)
            .add("investmentShare=" + investmentShare)
            .add("investmentSize=" + investmentSize)
            .add("purchaseSize=" + purchaseSize)
            .add("reservationMode=" + reservationMode)
            .add("sellingMode=" + sellingMode)
            .add("targetPortfolioSize=" + targetPortfolioSize)
            .add("exitProperties=" + exitProperties)
            .toString();
    }
}
