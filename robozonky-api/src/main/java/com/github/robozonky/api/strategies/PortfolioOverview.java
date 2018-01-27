/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.api.strategies;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.enums.Rating;

/**
 * Class with some aggregate statistics about user's portfolio. Used primarily as the main input into
 * {@link InvestmentStrategy}.
 */
public class PortfolioOverview {

    private final int czkAvailable, czkInvested;
    private final Map<Rating, Integer> czkInvestedPerRating;
    private final Map<Rating, BigDecimal> sharesOnInvestment;

    private PortfolioOverview(final BigDecimal czkAvailable, final Map<Rating, Integer> czkInvestedPerRating) {
        this.czkAvailable = czkAvailable.intValue();
        this.czkInvested = PortfolioOverview.sum(czkInvestedPerRating.values());
        if (this.czkInvested == 0) {
            this.czkInvestedPerRating = Collections.emptyMap();
            this.sharesOnInvestment = Collections.emptyMap();
        } else {
            this.czkInvestedPerRating = new EnumMap<>(czkInvestedPerRating);
            this.sharesOnInvestment = Arrays.stream(Rating.values()).collect(Collectors.toMap(
                    Function.identity(),
                    r -> {
                        final BigDecimal invested = BigDecimal.valueOf(this.czkInvested);
                        final BigDecimal investedPerRating = BigDecimal.valueOf(this.getCzkInvested(r));
                        return investedPerRating.divide(invested, 4, RoundingMode.HALF_EVEN).stripTrailingZeros();
                    })
            );
        }
    }

    private static int sum(final Collection<Integer> vals) {
        return vals.stream().reduce(0, (a, b) -> a + b);
    }

    /**
     * Prepare an immutable portfolio overview, based on the provided information.
     * @param balance Current available balance in the wallet.
     * @param investments All active investments incl. blocked amounts.
     * @return Never null.
     */
    public static PortfolioOverview calculate(final BigDecimal balance, final Stream<Investment> investments) {
        // FIXME replace summing by reduction, to be able to use BigDecimal precision
        final Map<Rating, Integer> amounts = investments
                .collect(Collectors.groupingBy(Investment::getRating,
                                               Collectors.summingInt(i -> i.getRemainingPrincipal().intValue())));
        return calculate(balance, amounts);
    }

    public static PortfolioOverview calculate(final BigDecimal balance, final Map<Rating, Integer> amounts) {
        return new PortfolioOverview(balance, amounts);
    }

    /**
     * Available balance in the wallet.
     * @return Amount in CZK.
     */
    public int getCzkAvailable() {
        return this.czkAvailable;
    }

    /**
     * Sum total of all amounts yet unpaid.
     * @return Amount in CZK.
     */
    public int getCzkInvested() {
        return this.czkInvested;
    }

    /**
     * Amount yet unpaid in a given rating.
     * @param r Rating in question.
     * @return Amount in CZK.
     */
    public int getCzkInvested(final Rating r) {
        return this.czkInvestedPerRating.getOrDefault(r, 0);
    }

    /**
     * Retrieve the amounts due in a given rating, divided by {@link #getCzkInvested()}.
     * @param r Rating in question.
     * @return Share of the given rating on overall investments.
     */
    public BigDecimal getShareOnInvestment(final Rating r) {
        return this.sharesOnInvestment.getOrDefault(r, BigDecimal.ZERO);
    }
}
