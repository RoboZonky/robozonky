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

package com.github.robozonky.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.stream.LongStream;

public class NumberUtil {

    private NumberUtil() {
        // no instances
    }

    public static double toCurrency(final BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_EVEN).doubleValue();
    }

    public static boolean hasAdditions(final long[] original, final long... current) {
        return LongStream.of(current).anyMatch(i -> LongStream.of(original).noneMatch(j -> i == j));
    }

}
