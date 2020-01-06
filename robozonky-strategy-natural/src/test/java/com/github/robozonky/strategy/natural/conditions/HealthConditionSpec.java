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

package com.github.robozonky.strategy.natural.conditions;

import java.util.Optional;

import com.github.robozonky.api.remote.enums.LoanHealth;
import com.github.robozonky.strategy.natural.Wrapper;

import static org.mockito.Mockito.*;

class HealthConditionSpec implements AbstractEnumeratedConditionTest.ConditionSpec<LoanHealth> {

    @Override
    public AbstractEnumeratedCondition<LoanHealth> getImplementation() {
        return new HealthCondition();
    }

    @Override
    public Wrapper<?> getMocked() {
        final Wrapper<?> w = mock(Wrapper.class);
        when(w.getHealth()).thenReturn(Optional.of(this.getTriggerItem()));
        return w;
    }

    @Override
    public LoanHealth getTriggerItem() {
        return LoanHealth.HEALTHY;
    }

    @Override
    public LoanHealth getNotTriggerItem() {
        return LoanHealth.HISTORICALLY_IN_DUE;
    }
}
