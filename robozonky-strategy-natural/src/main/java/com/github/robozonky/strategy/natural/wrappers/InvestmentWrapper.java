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

import com.github.robozonky.api.Money;
import com.github.robozonky.api.Ratio;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.LoanHealthStats;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.enums.DetailLabel;
import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class InvestmentWrapper extends AbstractLoanWrapper<InvestmentDescriptor> {

    private final Investment investment;

    public InvestmentWrapper(final InvestmentDescriptor original, final PortfolioOverview portfolioOverview) {
        super(original, portfolioOverview);
        this.investment = original.item();
    }

    @Override
    public long getId() {
        return investment.getId();
    }

    @Override
    public long getLoanId() {
        return investment.getLoan()
            .getId();
    }

    @Override
    public boolean isInsuranceActive() {
        return investment.getLoan()
            .getDetailLabels()
            .contains(DetailLabel.CURRENTLY_INSURED);
    }

    @Override
    public MainIncomeType getMainIncomeType() {
        return getLoan().getMainIncomeType();
    }

    @Override
    public Ratio getInterestRate() {
        return investment.getLoan()
            .getInterestRate();
    }

    @Override
    public Ratio getRevenueRate() {
        return estimateRevenueRate();
    }

    @Override
    public Purpose getPurpose() {
        return getLoan().getPurpose();
    }

    @Override
    public Rating getRating() {
        return investment.getLoan()
            .getRating();
    }

    @Override
    public int getOriginalTermInMonths() {
        return investment.getLoan()
            .getPayments()
            .getTotal();
    }

    @Override
    public int getRemainingTermInMonths() {
        return investment.getLoan()
            .getPayments()
            .getUnpaid();
    }

    @Override
    public int getOriginalAmount() {
        return getLoan()
            .getAmount()
            .getValue()
            .intValue();
    }

    @Override
    public int getOriginalAnnuity() {
        return investment.getLoan()
            .getAnnuity()
            .orElseThrow(() -> new IllegalStateException("Investment has no annuity: " + investment))
            .getValue()
            .intValue();
    }

    @Override
    public BigDecimal getRemainingPrincipal() {
        return investment.getPrincipal()
            .getUnpaid()
            .getValue();
    }

    @Override
    public Optional<BigDecimal> getReturns() { // FIXME add penalties when Zonky API supports it again
        var interest = investment.getInterest()
            .getPaid();
        var principal = investment.getPrincipal()
            .getPaid();
        return Optional.of(interest.add(principal)
            .add(interest)
            .getValue());
    }

    @Override
    public Optional<BigDecimal> getSellFee() {
        var fee = investment.getSmpSellInfo()
            .map(sellInfo -> sellInfo.getFee()
                .getValue())
            .orElse(Money.ZERO)
            .getValue();
        return Optional.of(fee);
    }

    @Override
    public Optional<LoanHealth> getHealth() {
        return investment.getLoan()
            .getHealthStats()
            .map(LoanHealthStats::getLoanHealthInfo);
    }

    @Override
    public Optional<BigDecimal> getOriginalPurchasePrice() {
        return Optional.of(investment.getSmpSellInfo()
            .map(sellInfo -> sellInfo.getBoughtFor()
                .getValue())
            .orElseGet(this::getRemainingPrincipal));
    }

    @Override
    public Optional<BigDecimal> getPrice() {
        var price = investment.getSmpSellInfo()
            .orElseThrow(() -> new IllegalStateException("Investment has no sell info: " + investment))
            .getSellPrice();
        return Optional.of(price.getValue());
    }

    @Override
    public Optional<BigDecimal> getDiscount() {
        var discount = investment.getSmpSellInfo()
            .map(SellInfo::getDiscount)
            .orElse(Ratio.ZERO);
        var result = discount.apply(Money.from(getRemainingPrincipal()));
        return Optional.of(result.getValue());
    }

    @Override
    public OptionalInt getCurrentDpd() {
        return investment.getLoan()
            .getHealthStats()
            .map(s -> OptionalInt.of(s.getCurrentDaysDue()))
            .orElseGet(OptionalInt::empty);
    }

    @Override
    public OptionalInt getLongestDpd() {
        return investment.getLoan()
            .getHealthStats()
            .map(s -> OptionalInt.of(s.getLongestDaysDue()))
            .orElseGet(OptionalInt::empty);
    }

    @Override
    public OptionalInt getDaysSinceDpd() {
        return investment.getLoan()
            .getHealthStats()
            .map(s -> OptionalInt.of(s.getDaysSinceLastInDue()))
            .orElseGet(OptionalInt::empty);
    }

    @Override
    public String toString() {
        return "Wrapper for loan #" + getLoanId() + ", investment #" + getId();
    }
}
