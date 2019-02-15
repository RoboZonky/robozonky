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

package com.github.robozonky.api.remote.enums;

public enum LoanTermInterval {

    FROM_0_TO_12(1, 12),
    FROM_13_TO_24(13, 24),
    FROM_25_TO_36(25, 36),
    FROM_37_TO_48(37, 48),
    FROM_49_TO_60(49, 60),
    FROM_61_TO_72(61, 72),
    FROM_73_TO_84(73, 84);

    private final int minInclusive;
    private final int maxInclusive;

    LoanTermInterval(final int minInclusive, final int maxInclusive) {
        this.minInclusive = minInclusive;
        this.maxInclusive = maxInclusive;
    }

    public int getMinInclusive() {
        return minInclusive;
    }

    public int getMaxInclusive() {
        return maxInclusive;
    }
}
