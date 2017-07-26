/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.app.investing;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.common.remote.Zonky;

public class DirectInvestmentMode extends AbstractInvestmentMode {

    private final int loanId, loanAmount;

    public DirectInvestmentMode(final Authenticated auth, final Investor.Builder builder, final boolean isFaultTolerant,
                                final int loanId, final int loanAmount) {
        super(auth, builder, isFaultTolerant);
        this.loanId = loanId;
        this.loanAmount = loanAmount;
    }

    @Override
    protected void openMarketplace(final Consumer<Collection<Loan>> target) {
        target.accept(Collections.emptyList());
    }

    @Override
    protected Function<Collection<LoanDescriptor>, Collection<Investment>> getInvestor() {
        final Function<Zonky, Collection<Investment>> op = (zonky) -> {
            final Loan l = zonky.getLoan(loanId);
            final LoanDescriptor d = new LoanDescriptor(l);
            return d.recommend(loanAmount, false)
                    .map(r -> Session.invest(getInvestorBuilder(), zonky, new DirectInvestmentCommand(r)))
                    .orElse(Collections.emptyList());
        };
        return (marketplace) -> this.getAuthenticated().call(op);
    }

    @Override
    public Optional<Collection<Investment>> get() {
        return this.execute(null);
    }
}
