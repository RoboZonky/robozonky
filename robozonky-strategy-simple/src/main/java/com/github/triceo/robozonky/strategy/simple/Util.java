/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.simple;

import java.math.BigDecimal;
import java.util.StringJoiner;

class Util {

    public static boolean isBetweenZeroAndOne(final BigDecimal target) {
        return Util.isBetweenAAndB(target, BigDecimal.ZERO, BigDecimal.ONE);
    }

    public static boolean isBetweenAAndB(final BigDecimal target, final BigDecimal a, final BigDecimal b) {
        return !(target.compareTo(a) < 0 || target.compareTo(b) > 0);
    }

    public static String join(final String left, final String right) {
        return new StringJoiner(".").add(left).add(right).toString();
    }

}
