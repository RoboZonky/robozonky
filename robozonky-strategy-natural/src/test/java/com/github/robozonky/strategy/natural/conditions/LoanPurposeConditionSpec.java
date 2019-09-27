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

package com.github.robozonky.strategy.natural.conditions;

import com.github.robozonky.api.remote.entities.Loan;
import com.github.robozonky.api.remote.enums.Purpose;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.strategy.natural.Wrapper;
import com.github.robozonky.test.mock.MockLoanBuilder;

import static org.mockito.Mockito.mock;

class LoanPurposeConditionSpec implements AbstractEnumeratedConditionTest.ConditionSpec<Purpose> {

    private static final PortfolioOverview FOLIO = mock(PortfolioOverview.class);

    @Override
    public AbstractEnumeratedCondition<Purpose> getImplementation() {
        return new LoanPurposeCondition();
    }

    @Override
    public Wrapper<?> getMocked() {
        final Loan loan = new MockLoanBuilder().setPurpose(this.getTriggerItem()).build();
        return Wrapper.wrap(new LoanDescriptor(loan), FOLIO);
    }

    @Override
    public Purpose getTriggerItem() {
        return Purpose.AUTO_MOTO;
    }

    @Override
    public Purpose getNotTriggerItem() {
        return Purpose.TRAVEL;
    }
}
