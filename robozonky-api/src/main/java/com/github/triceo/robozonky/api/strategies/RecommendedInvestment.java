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

package com.github.triceo.robozonky.api.strategies;

import java.math.BigDecimal;
import java.util.Objects;

import com.github.triceo.robozonky.api.remote.entities.Investment;

public final class RecommendedInvestment
        implements Recommended<RecommendedInvestment, InvestmentDescriptor, Investment> {

    private final InvestmentDescriptor descriptor;

    RecommendedInvestment(final InvestmentDescriptor participationDescriptor) {
        this.descriptor = participationDescriptor;
    }

    @Override
    public InvestmentDescriptor descriptor() {
        return descriptor;
    }

    @Override
    public BigDecimal amount() {
        return descriptor.item().getRemainingPrincipal();
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
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

