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

package com.github.robozonky.strategy.natural.conditions;

import java.util.Optional;

import com.github.robozonky.strategy.natural.InvestmentWrapper;
import com.github.robozonky.strategy.natural.LoanWrapper;

public class InsuranceCondition extends MarketplaceFilterConditionImpl {

    public static final MarketplaceFilterCondition ACTIVE = new InsuranceCondition(true), INACTIVE = ACTIVE.negate();

    private final boolean expected;

    private InsuranceCondition(final boolean expected) {
        this.expected = expected;
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of(expected ? "With insurance." : "Without insurance.");
    }

    @Override
    public boolean test(final LoanWrapper wrapper) {
        return wrapper.isInsuranceActive() == expected;
    }

    @Override
    public boolean test(final InvestmentWrapper wrapper) {
        return wrapper.isInsuranceActive() == expected;
    }
}
