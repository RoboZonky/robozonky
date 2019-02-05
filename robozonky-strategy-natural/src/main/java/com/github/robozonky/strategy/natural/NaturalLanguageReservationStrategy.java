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
import java.util.Collection;
import java.util.stream.Stream;

import com.github.robozonky.api.remote.entities.Restrictions;
import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.api.strategies.RecommendedLoan;
import com.github.robozonky.api.strategies.ReservationStrategy;
import com.github.robozonky.api.strategies.ReservationStrategyType;

class NaturalLanguageReservationStrategy extends AbstractNaturalLanguageInvestmentStrategy
        implements ReservationStrategy {

    public NaturalLanguageReservationStrategy(final ParsedStrategy p) {
        super(p);
    }

    @Override
    protected boolean needsConfirmation(final LoanDescriptor loanDescriptor) {
        return false;
    }

    @Override
    protected int recommendAmount(final MarketplaceLoan loan, final BigDecimal balance,
                                  final Restrictions restrictions) {
        return restrictions.getMinimumInvestmentAmount();
    }

    @Override
    public ReservationStrategyType getType() {
        return null;
    }

    @Override
    public Stream<RecommendedLoan> recommend(final Collection<LoanDescriptor> available,
                                             final PortfolioOverview portfolio, final Restrictions restrictions) {
        return super.recommend(available, portfolio, restrictions);
    }
}
