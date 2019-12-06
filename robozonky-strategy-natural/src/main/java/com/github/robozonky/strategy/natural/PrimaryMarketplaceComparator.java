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

import java.util.Comparator;

import com.github.robozonky.api.remote.entities.BaseLoan;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;

final class PrimaryMarketplaceComparator implements Comparator<LoanDescriptor> {

    private static final Comparator<Loan> BASE = Comparator.comparing(Loan::getDatePublished, Comparator.reverseOrder())
            .thenComparing(BaseLoan::getNonReservedRemainingInvestment, Comparator.reverseOrder());
    private final Comparator<Loan> comparator;

    public PrimaryMarketplaceComparator(Comparator<Rating> ratingByDemandComparator) {
        this.comparator = Comparator.comparing(Loan::getRating, ratingByDemandComparator)
                .thenComparing(BASE);
    }

    @Override
    public int compare(final LoanDescriptor loan1, final LoanDescriptor loan2) {
        return comparator.compare(loan1.item(), loan2.item());
    }
}
