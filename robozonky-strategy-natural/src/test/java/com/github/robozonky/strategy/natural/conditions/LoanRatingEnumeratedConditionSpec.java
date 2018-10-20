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
import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.strategy.natural.Wrapper;

class LoanRatingEnumeratedConditionSpec implements AbstractEnumeratedConditionTest.ConditionSpec<Rating> {

    @Override
    public AbstractEnumeratedCondition<Rating> getImplementation() {
        return new LoanRatingEnumeratedCondition();
    }

    @Override
    public Wrapper<?> getMocked() {
        final Loan loan = Loan.custom().setRating(this.getTriggerItem()).build();
        return Wrapper.wrap(new LoanDescriptor(loan));
    }

    @Override
    public Rating getTriggerItem() {
        return Rating.A;
    }

    @Override
    public Rating getNotTriggerItem() {
        return Rating.D;
    }
}
