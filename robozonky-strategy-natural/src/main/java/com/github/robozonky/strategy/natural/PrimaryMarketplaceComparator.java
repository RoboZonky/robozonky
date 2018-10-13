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

import java.util.Comparator;

import com.github.robozonky.api.remote.entities.sanitized.MarketplaceLoan;
import com.github.robozonky.api.strategies.LoanDescriptor;

/**
 * Loan ordering such that it maximizes the chances the loan is still available on the marketplace when the investment
 * operation is triggered. In other words, this tries to implement a heuristic of "most popular insured loans first."
 */
class PrimaryMarketplaceComparator implements Comparator<LoanDescriptor> {

    private static final Comparator<MarketplaceLoan>
            MOST_RECENT_FIRST = Comparator.comparing(MarketplaceLoan::getDatePublished).reversed(),
            BIGGEST_FIRST = Comparator.comparing(MarketplaceLoan::getRemainingInvestment).reversed(),
            INSURED_FIRST = Comparator.comparing(MarketplaceLoan::isInsuranceActive).reversed(),
            FINAL = INSURED_FIRST.thenComparing(MOST_RECENT_FIRST).thenComparing(BIGGEST_FIRST);

    @Override
    public int compare(final LoanDescriptor loan1, final LoanDescriptor loan2) {
        return FINAL.compare(loan1.item(), loan2.item());
    }
}
