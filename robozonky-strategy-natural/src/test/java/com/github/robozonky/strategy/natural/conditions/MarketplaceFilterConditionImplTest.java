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

import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.strategy.natural.Wrapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MarketplaceFilterConditionImplTest {

    @Test
    void doubleNegation() {
        final MarketplaceFilterCondition negated = AlwaysAcceptingCondition.INSTANCE.invert();
        assertThat(negated.test(Wrapper.wrap(new LoanDescriptor(Loan.custom().build())))).isFalse();
        final MarketplaceFilterCondition doubleNegated = negated.invert();
        assertThat(doubleNegated).isSameAs(AlwaysAcceptingCondition.INSTANCE);
    }
}
