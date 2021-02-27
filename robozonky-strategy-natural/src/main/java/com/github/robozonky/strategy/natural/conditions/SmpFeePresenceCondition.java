/*
 * Copyright 2021 The RoboZonky Project
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

public final class SmpFeePresenceCondition extends AbstractBooleanCondition {

    public static final MarketplaceFilterCondition PRESENT = new SmpFeePresenceCondition();
    public static final MarketplaceFilterCondition NOT_PRESENT = PRESENT.negate();

    private SmpFeePresenceCondition() {
        super(w -> w.getSellFee()
            .map(fee -> fee.signum() > 0)
            .orElse(false), true, true);
    }

    @Override
    public Optional<String> getDescription() {
        return Optional.of("Sale fee present.");
    }
}
