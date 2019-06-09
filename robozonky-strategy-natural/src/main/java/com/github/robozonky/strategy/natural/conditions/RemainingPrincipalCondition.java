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

import java.math.BigDecimal;

import com.github.robozonky.strategy.natural.Wrapper;

public final class RemainingPrincipalCondition extends AbstractRangeCondition<BigDecimal> {

    private RemainingPrincipalCondition(final NewRangeCondition<BigDecimal> condition) {
        super(condition);
    }

    public static RemainingPrincipalCondition lessThan(final int threshold) {
        final NewRangeCondition<BigDecimal> c = NewRangeCondition.lessThan(Wrapper::getRemainingPrincipal,
                                                                           PRINCIPAL_DOMAIN,
                                                                           BigDecimal.valueOf(threshold));
        return new RemainingPrincipalCondition(c);
    }

    public static RemainingPrincipalCondition moreThan(final int threshold) {
        final NewRangeCondition<BigDecimal> c = NewRangeCondition.moreThan(Wrapper::getRemainingPrincipal,
                                                                           PRINCIPAL_DOMAIN,
                                                                           BigDecimal.valueOf(threshold));
        return new RemainingPrincipalCondition(c);
    }

    public static RemainingPrincipalCondition exact(final int minimumThreshold,
                                                    final int maximumThreshold) {
        final NewRangeCondition<BigDecimal> c = NewRangeCondition.exact(Wrapper::getRemainingPrincipal,
                                                                        PRINCIPAL_DOMAIN,
                                                                        BigDecimal.valueOf(minimumThreshold),
                                                                        BigDecimal.valueOf(maximumThreshold));
        return new RemainingPrincipalCondition(c);
    }
}
