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

import com.github.robozonky.api.remote.enums.Region;
import com.github.robozonky.strategy.natural.LoanWrapper;

import static org.mockito.Mockito.*;

class BorrowerRegionConditionSpec implements ConditionSpec<Region, LoanWrapper> {

    private AbstractEnumeratedCondition<Region> impl;

    @Override
    public AbstractEnumeratedCondition<Region> newImplementation() {
        this.impl = new BorrowerRegionCondition();
        return impl;
    }


    @Override
    public LoanWrapper getMocked() {
        final LoanWrapper w = mock(LoanWrapper.class);
        when(w.getRegion()).thenReturn(this.getTriggerItem());
        return w;
    }

    @Override
    public Region getTriggerItem() {
        return Region.JIHOCESKY;
    }

    @Override
    public Region getNotTriggerItem() {
        return Region.JIHOMORAVSKY;
    }

    @Override
    public boolean test(final LoanWrapper wrapper) {
        return impl.test(wrapper);
    }
}
