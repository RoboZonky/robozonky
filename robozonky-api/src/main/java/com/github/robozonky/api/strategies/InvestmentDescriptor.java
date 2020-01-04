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

package com.github.robozonky.api.strategies;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.entities.SellInfo;
import io.vavr.Lazy;

/**
 * Carries metadata regarding an {@link Investment}.
 */
public final class InvestmentDescriptor implements Descriptor<RecommendedInvestment, InvestmentDescriptor, Investment> {

    private final Investment investment;
    private final Lazy<Loan> related;
    private final Lazy<SellInfo> sellInfo;

    /**
     *
     * @param investment
     * @param related Provided as a {@link Supplier} in order to allow the calling code to retrieve the (likely remote)
     * entity on-demand.
     */
    public InvestmentDescriptor(final Investment investment, final Supplier<Loan> related) {
        this(investment, related, null);
    }

    /**
     *
     * @param investment
     * @param related Provided as a {@link Supplier} in order to allow the calling code to retrieve the (likely remote)
     * entity on-demand.
     * @param sellInfo Provided as a {@link Supplier} in order to allow the calling code to retrieve the (likely remote)
     * entity on-demand. Null means no such information exists.
     */
    public InvestmentDescriptor(final Investment investment, final Supplier<Loan> related,
                                final Supplier<SellInfo> sellInfo) {
        this.investment = investment;
        this.related = Lazy.of(related);
        this.sellInfo = sellInfo == null ? null : Lazy.of(sellInfo);
    }

    @Override
    public Investment item() {
        return investment;
    }

    @Override
    public Loan related() {
        return related.get();
    }

    public Optional<SellInfo> sellInfo() {
        return sellInfo == null ? Optional.empty() : Optional.of(sellInfo.get());
    }

    private Money getRemainingPrincipal() {
        return investment.getRemainingPrincipal().orElseThrow();
    }

    public Optional<RecommendedInvestment> recommend() {
        return recommend(getRemainingPrincipal());
    }

    @Override
    public Optional<RecommendedInvestment> recommend(final Money amount) {
        if (Objects.equals(amount, getRemainingPrincipal())) {
            return Optional.of(new RecommendedInvestment(this, amount));
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        final InvestmentDescriptor that = (InvestmentDescriptor) o;
        return Objects.equals(investment, that.investment);
    }

    @Override
    public String toString() {
        return "InvestmentDescriptor{" +
                "investment=" + investment +
                '}';
    }

    @Override
    public int hashCode() {
        return Objects.hash(investment);
    }
}

