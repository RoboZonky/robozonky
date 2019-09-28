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

package com.github.robozonky.api.strategies;

import com.github.robozonky.api.Money;
import com.github.robozonky.api.remote.entities.Investment;

import java.util.Objects;

public final class RecommendedInvestment
        implements Recommended<RecommendedInvestment, InvestmentDescriptor, Investment> {

    private final InvestmentDescriptor descriptor;
    private final Money amount;

    RecommendedInvestment(final InvestmentDescriptor participationDescriptor, final Money amount) {
        this.descriptor = participationDescriptor;
        this.amount = amount;
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
        final RecommendedInvestment that = (RecommendedInvestment) o;
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

