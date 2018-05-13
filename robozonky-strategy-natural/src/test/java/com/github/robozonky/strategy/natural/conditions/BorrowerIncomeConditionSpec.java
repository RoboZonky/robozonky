/*
 * Copyright 2017 The RoboZonky Project
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
import com.github.robozonky.api.remote.enums.MainIncomeType;
import com.github.robozonky.strategy.natural.LoanWrapper;

class BorrowerIncomeConditionSpec implements ConditionSpec<MainIncomeType, LoanWrapper> {

    private AbstractEnumeratedCondition<MainIncomeType> impl;

    @Override
    public AbstractEnumeratedCondition<MainIncomeType> newImplementation() {
        this.impl = new BorrowerIncomeCondition();
        return impl;
    }

    @Override
    public LoanWrapper getMocked() {
        final Loan loan = Loan.custom().setMainIncomeType(this.getTriggerItem()).build();
        return new LoanWrapper(loan);
    }

    @Override
    public MainIncomeType getTriggerItem() {
        return MainIncomeType.EMPLOYMENT;
    }

    @Override
    public MainIncomeType getNotTriggerItem() {
        return MainIncomeType.ENTREPRENEUR;
    }

    @Override
    public boolean test(final LoanWrapper wrapper) {
        return impl.test(wrapper);
    }
}
