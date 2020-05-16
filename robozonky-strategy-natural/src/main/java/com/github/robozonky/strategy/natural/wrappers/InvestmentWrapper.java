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
import java.util.Optional;
import java.util.OptionalInt;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class InvestmentWrapper extends AbstractLoanWrapper<InvestmentDescriptor> {

    private final Investment investment;
    private final Supplier<Optional<SellInfo>> sellInfo;

    public InvestmentWrapper(final InvestmentDescriptor original, final PortfolioOverview portfolioOverview) {
        super(original, portfolioOverview);
        this.investment = original.item();
        this.sellInfo = original::sellInfo;
    }

    @Override
    public long getId() {
        return investment.getId();
    }

    @Override
    public boolean isInsuranceActive() {
        return investment.isInsuranceActive();
    }

    @Override
    public MainIncomeType getMainIncomeType() {
        return getLoan().getMainIncomeType();
    }

    @Override
    public Ratio getInterestRate() {
        return investment.getInterestRate();
    }

    @Override
    public Ratio getRevenueRate() {
        return investment.getRevenueRate()
            .orElseGet(() -> estimateRevenueRate(investment.getInvestmentDate()));
    }

    @Override
    public Purpose getPurpose() {
        return getLoan().getPurpose();
    }

    @Override
    public Rating getRating() {
        return investment.getRating();
    }

    @Override
    public int getOriginalTermInMonths() {
        return investment.getLoanTermInMonth();
    }

    @Override
    public int getRemainingTermInMonths() {
        return investment.getRemainingMonths();
    }

    @Override
    public int getOriginalAmount() {
        return investment.getLoanAmount()
            .getValue()
            .intValue();
    }

    @Override
    public int getOriginalAnnuity() {
        return investment.getLoanAnnuity()
            .getValue()
            .intValue();
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        return investment.getRemainingPrincipal()
            .orElseThrow()
            .getValue();
    }

    @Override
    public Optional<BigDecimal> getReturns() {
        var interest = investment.getPaidInterest();
        var principal = investment.getPaidPrincipal();
        var penalties = investment.getPaidPenalty();
        return Optional.of(interest.add(principal)
            .add(penalties)
            .getValue());
    }

    @Override
    public Optional<BigDecimal> getSellFee() {
        var fee = investment.getSmpFee()
            .orElseGet(() -> sellInfo.get()
                .map(si -> si.getPriceInfo()
                    .getFee()
                    .getValue())
                .orElse(Money.ZERO))
            .getValue();
        return Optional.of(fee);
    }

    private <T> T extractOrFail(final Function<SellInfo, T> extractor) {
        return sellInfo.get()
            .map(extractor)
            .orElseThrow();
    }

    @Override
    public Optional<LoanHealth> getHealth() {
        var healthInfo = investment.getLoanHealthInfo()
            .orElseGet(() -> extractOrFail(si -> si.getLoanHealthStats()
                .getLoanHealthInfo()));
        return Optional.of(healthInfo);
    }

    @Override
    public Optional<BigDecimal> getOriginalPurchasePrice() {
        return Optional.of(investment.getPurchasePrice()
            .getValue());
    }

    @Override
    public Optional<BigDecimal> getPrice() {
        var price = investment.getSmpPrice()
            .orElseGet(() -> extractOrFail(si -> si.getPriceInfo()
                .getSellPrice()));
        return Optional.of(price.getValue());
    }

    @Override
    public Optional<BigDecimal> getDiscount() {
        var discount = sellInfo.get()
            .map(si -> si.getPriceInfo()
                .getDiscount())
            .orElse(Ratio.ZERO);
        var result = discount.apply(Money.from(getRemainingPrincipal()));
        return Optional.of(result.getValue());
    }

    @Override
    public OptionalInt getCurrentDpd() {
        var currentDpd = investment.getLegalDpd()
            .orElseGet(() -> sellInfo.get()
                .map(si -> si.getLoanHealthStats()
                    .getCurrentDaysDue())
                .orElse(0));
        return OptionalInt.of(currentDpd);
    }

    @Override
    public OptionalInt getLongestDpd() {
        var maxDpd = investment.getLegalDpd()
            .orElseGet(() -> sellInfo.get()
                .map(si -> si.getLoanHealthStats()
                    .getLongestDaysDue())
                .orElse(0));
        return OptionalInt.of(maxDpd);
    }

    @Override
    public OptionalInt getDaysSinceDpd() {
        var daysSinceLastDpd = sellInfo.get()
            .map(si -> si.getLoanHealthStats()
                .getDaysSinceLastInDue())
            .orElse(0);
        return OptionalInt.of(daysSinceLastDpd);
    }

    @Override
    public String toString() {
        return "Wrapper for loan #" + investment.getLoanId() + ", investment #" + getId();
    }
}
