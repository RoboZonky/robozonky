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

package com.github.robozonky.internal.remote;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Supplier;

import com.github.robozonky.api.remote.entities.Amounts;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.InvestmentLoanData;
import com.github.robozonky.api.remote.entities.SellInfo;
import com.github.robozonky.api.remote.enums.SellStatus;
import com.github.robozonky.internal.util.functional.Memoizer;

/**
 * Some things in Zonky API are optional and only available in some situations. Case in point:
 * <ul>
 * <li>{@link Investment#getSmpSellInfo()}.</li>
 * <li>{@link InvestmentLoanData#getHealthStats()}.</li>
 * </ul>
 * The point of this class is to hide this complexity from the rest of the code.
 * When the data is missing, it will be automatically fetched in the background.
 */
final class AutoExtendingInvestmentImpl implements Investment {

    private final Investment delegate;
    private final Supplier<Investment> fullInvestmentSupplier;
    private final InvestmentLoanData loanDataDelegate;

    public AutoExtendingInvestmentImpl(Investment investment, Zonky zonky) {
        this.delegate = investment;
        this.fullInvestmentSupplier = Memoizer.memoize(() -> zonky.getInvestment(investment.getId()));
        this.loanDataDelegate = new AutoExtendingInvestmentLoanDataImpl(investment.getLoan(), fullInvestmentSupplier);
    }

    @Override
    public long getId() {
        return delegate.getId();
    }

    @Override
    public InvestmentLoanData getLoan() {
        return loanDataDelegate;
    }

    @Override
    public Optional<SellInfo> getSmpSellInfo() {
        return delegate.getSmpSellInfo()
            .or(() -> fullInvestmentSupplier.get()
                .getSmpSellInfo());
    }

    @Override
    public Amounts getPrincipal() {
        return delegate.getPrincipal();
    }

    @Override
    public Amounts getInterest() {
        return delegate.getInterest();
    }

    @Override
    public SellStatus getSellStatus() {
        return delegate.getSellStatus();
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", AutoExtendingInvestmentImpl.class.getSimpleName() + "[", "]")
            .add("delegate=" + delegate)
            .toString();
    }
}
