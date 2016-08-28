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

import java.util.StringJoiner;

class StrategyFileProperties {

    private static final String PART_MINIMUM = "minimum";
    private static final String PART_MAXIMUM = "maximum";
    private static final String PART_ASK = "ask";
    private static final String PART_TERM = "term";
    private static final String PART_LOAN_AMOUNT = "loanAmount";
    private static final String PART_LOAN_SHARE = "loanShare";
    public static final String TARGET_SHARE = "targetShare";
    public static final String PREFER_LONGER_TERMS = "preferLongerTerms";
    public static final String MINIMUM_BALANCE = StrategyFileProperties.getMinimum("balance");
    public static final String MAXIMUM_INVESTMENT = StrategyFileProperties.getMaximum("investment");
    public static final String MINIMUM_TERM = StrategyFileProperties.getMinimum(StrategyFileProperties.PART_TERM);
    public static final String MINIMUM_ASK = StrategyFileProperties.getMinimum(StrategyFileProperties.PART_ASK);
    public static final String MAXIMUM_TERM = StrategyFileProperties.getMaximum(StrategyFileProperties.PART_TERM);
    public static final String MAXIMUM_ASK = StrategyFileProperties.getMaximum(StrategyFileProperties.PART_ASK);
    public static final String MINIMUM_LOAN_AMOUNT =
            StrategyFileProperties.getMinimum(StrategyFileProperties.PART_LOAN_AMOUNT);
    public static final String MINIMUM_LOAN_SHARE =
            StrategyFileProperties.getMinimum(StrategyFileProperties.PART_LOAN_SHARE);
    public static final String MAXIMUM_LOAN_AMOUNT =
            StrategyFileProperties.getMaximum(StrategyFileProperties.PART_LOAN_AMOUNT);
    public static final String MAXIMUM_LOAN_SHARE =
            StrategyFileProperties.getMaximum(StrategyFileProperties.PART_LOAN_SHARE);

    private static String getMinimum(final String str) {
        return StrategyFileProperties.get(StrategyFileProperties.PART_MINIMUM, str);
    }

    private static String getMaximum(final String str) {
        return StrategyFileProperties.get(StrategyFileProperties.PART_MAXIMUM, str);
    }

    private static String get(final String prefix, final String str) {
        return new StringJoiner("")
                .add(prefix.toLowerCase())
                .add(str.substring(0, 1).toUpperCase())
                .add(str.substring(1))
                .toString();
    }


}
