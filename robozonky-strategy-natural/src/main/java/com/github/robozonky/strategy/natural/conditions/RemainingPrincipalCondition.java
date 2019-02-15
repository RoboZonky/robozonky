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

import com.github.robozonky.strategy.natural.Wrapper;

public class RemainingPrincipalCondition extends AbstractRangeCondition {

    public RemainingPrincipalCondition(final int fromInclusive, final int toInclusive) {
        super(Wrapper::getRemainingPrincipal, fromInclusive, toInclusive);
        if (fromInclusive < 0 || toInclusive < 0) {
            throw new IllegalArgumentException("Either values need to be 0 or more.");
        }
    }

    public RemainingPrincipalCondition(final int fromInclusive) {
        this(fromInclusive, Integer.MAX_VALUE);
    }
}
