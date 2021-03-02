/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.app.daemon;

import java.util.Objects;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;
import com.github.robozonky.api.strategies.InvestmentDescriptor;
import com.github.robozonky.internal.remote.entities.InvestmentImpl;

final class RecommendedInvestment implements Recommended<InvestmentDescriptor, Investment> {

    private final InvestmentDescriptor descriptor;
    private final Money amount;

    RecommendedInvestment(final InvestmentDescriptor investmentDescriptor) {
        this.descriptor = investmentDescriptor;
        this.amount = InvestmentImpl.determineSellPrice(investmentDescriptor.item());
    }

    @Override
    public InvestmentDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public Money amount() {
        return amount;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || !Objects.equals(getClass(), o.getClass())) {
            return false;
        }
        var that = (RecommendedInvestment) o;
        return Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
        return Objects.hash(descriptor);
    }

    @Override
    public String toString() {
        return "RecommendedInvestment{" +
                "descriptor=" + descriptor +
                '}';
    }
}
