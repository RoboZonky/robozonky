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

package com.github.robozonky.app.portfolio;

import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.common.remote.Zonky;

class PortfolioLoanProvider implements LoanProvider {

    private final Supplier<Optional<Portfolio>> portfolio;

    public PortfolioLoanProvider(final Supplier<Optional<Portfolio>> portfolio) {
        this.portfolio = portfolio;
    }

    @Override
    public Loan apply(final Integer loanId, final Zonky zonky) {
        return portfolio.get()
                .map(portfolio -> portfolio.getLoan(zonky, loanId))
                .orElseThrow(() -> new IllegalStateException("Cannot call on empty portfolio."));
    }
}
