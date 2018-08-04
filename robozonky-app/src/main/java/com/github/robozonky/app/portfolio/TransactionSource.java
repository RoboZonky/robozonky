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

package com.github.robozonky.app.portfolio;

import java.util.Arrays;
import java.util.List;

enum TransactionSource {

    REAL(),
    BLOCKED_AMOUNT(REAL),
    SYNTHETIC(REAL, BLOCKED_AMOUNT);

    private final List<TransactionSource> promotions;

    TransactionSource(final TransactionSource... possiblePromotions) {
        this.promotions = Arrays.asList(possiblePromotions);
    }

    boolean canBePromotedTo(final TransactionSource target) {
        if (this == target) { // FIXME is this the correct behavior?
            return true;
        }
        return promotions.contains(target);
    }

}
