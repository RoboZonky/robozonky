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

package com.github.robozonky.strategy.simple;

import java.util.Optional;
import java.util.StringJoiner;
import java.util.function.Function;

import com.github.robozonky.api.remote.enums.Rating;
import com.github.robozonky.internal.api.Defaults;

enum StrategyFileProperty {

    MINIMUM_BALANCE(StrategyFileProperty.getMinimum("balance")),
    MAXIMUM_INVESTMENT(StrategyFileProperty.getMaximum("investment")),
    PREFER_LONGER_TERMS("preferLongerTerms"),
    TARGET_SHARE("targetShare"),
    REQUIRE_CONFIRMATION("requireConfirmation"),
    MAXIMUM_SHARE(StrategyFileProperty.getMaximum("share")),
    MINIMUM_TERM(StrategyFileProperty.getMinimum(StrategyFileProperty.PART_TERM)),
    MAXIMUM_TERM(StrategyFileProperty.getMaximum(StrategyFileProperty.PART_TERM)),
    MINIMUM_ASK(StrategyFileProperty.getMinimum(StrategyFileProperty.PART_ASK)),
    MAXIMUM_ASK(StrategyFileProperty.getMaximum(StrategyFileProperty.PART_ASK)),
    MINIMUM_LOAN_AMOUNT(StrategyFileProperty.getMinimum(StrategyFileProperty.PART_LOAN_AMOUNT)),
    MAXIMUM_LOAN_AMOUNT(StrategyFileProperty.getMaximum(StrategyFileProperty.PART_LOAN_AMOUNT)),
    MINIMUM_LOAN_SHARE(StrategyFileProperty.getMinimum(StrategyFileProperty.PART_LOAN_SHARE)),
    MAXIMUM_LOAN_SHARE(StrategyFileProperty.getMaximum(StrategyFileProperty.PART_LOAN_SHARE));

    private static final String PART_MINIMUM = "minimum";
    private static final String PART_MAXIMUM = "maximum";
    private static final String PART_ASK = "ask";
    private static final String PART_TERM = "term";
    private static final String PART_LOAN_AMOUNT = "loanAmount";
    private static final String PART_LOAN_SHARE = "loanShare";

    private static String getMinimum(final String str) {
        return StrategyFileProperty.get(StrategyFileProperty.PART_MINIMUM, str);
    }

    private static String getMaximum(final String str) {
        return StrategyFileProperty.get(StrategyFileProperty.PART_MAXIMUM, str);
    }

    private static String get(final String prefix, final String str) {
        return new StringJoiner("")
                .add(prefix.toLowerCase(Defaults.LOCALE))
                .add(str.substring(0, 1).toUpperCase(Defaults.LOCALE))
                .add(str.substring(1))
                .toString();
    }

    private static <T> Optional<T> getValue(final String propertyName, final Function<String, Optional<T>> supplier) {
        return supplier.apply(propertyName);
    }

    private static String join(final String key, final String suffix) {
        return key + "." + suffix;
    }

    private final String key;

    StrategyFileProperty(final String key) {
        this.key = key;
    }

    public <T> Optional<T> getValue(final Function<String, Optional<T>> supplier) {
        return StrategyFileProperty.getValue(this.key, supplier);
    }

    public <T> T getValue(final Rating r, final Function<String, Optional<T>> supplier) {
        final String key = this.key;
        final String propertyName = StrategyFileProperty.join(key, r.name());
        return StrategyFileProperty.getValue(propertyName, supplier).orElseGet(() -> {
            final String fallbackPropertyName = StrategyFileProperty.join(key, "default");
            return StrategyFileProperty.getValue(fallbackPropertyName, supplier)
                    .orElseThrow(() -> new IllegalStateException("Investment strategy is incomplete. " +
                                                                         "Missing value for '" + key + "' and rating " +
                                                                         "'" + r + '\''));
        });
    }

}
