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

import com.github.robozonky.strategy.natural.Wrapper;

public class InsuranceCondition extends MarketplaceFilterConditionImpl {

    public static final MarketplaceFilterCondition ACTIVE = new InsuranceCondition(), INACTIVE = ACTIVE.negate();

    private InsuranceCondition() {
        // no external instances
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("With insurance.");
    }

    @Override
    public boolean test(final Wrapper<?> wrapper) {
        return wrapper.isInsuranceActive();
    }
}
