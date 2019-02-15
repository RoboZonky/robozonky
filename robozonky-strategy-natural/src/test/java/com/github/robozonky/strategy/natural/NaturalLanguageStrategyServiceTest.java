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

package com.github.robozonky.strategy.natural;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import com.github.robozonky.api.strategies.StrategyService;
import com.github.robozonky.internal.api.Defaults;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.DynamicNode;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.TestFactory;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.DynamicContainer.dynamicContainer;
import static org.junit.jupiter.api.DynamicTest.dynamicTest;

class NaturalLanguageStrategyServiceTest {

    private static final StrategyService SERVICE = new NaturalLanguageStrategyService();

    private static Stream<DynamicTest> forType(final Type type) {
        return Stream.of(
                dynamicTest("simplest possible", () -> simplest(type)),
                dynamicTest("complex", () -> complex(type)),
                dynamicTest("with disabled filters", () -> disabled(type)),
                dynamicTest("with enabled filters", () -> enabled(type)),
                dynamicTest("with some filters missing", () -> missingFilters1(type)),
                dynamicTest("with other filters missing", () -> missingFilters2(type)),
                dynamicTest("with yet other filters missing", () -> missingFilters3(type)),
                dynamicTest("with headers missing", () -> missingHeaders(type)),
                dynamicTest("all possible newlines", () -> newlines(type)),
                dynamicTest("newlines saved on Windows", () -> windows1250WindowsNewlines(type)),
                dynamicTest("newlines saved on Unix", () -> windows1250UnixNewlines(type)),
                dynamicTest("pure whitespace", () -> test(type))
        );
    }

    private static Optional<?> getStrategy(final Type strategy, final String str) {
        return strategy.getStrategy().apply(str);
    }

    private static void test(final Type strategy) throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("only-whitespace");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        assertThat(actualStrategy).isEmpty();
    }

    private static void newlines(final Type strategy) { // test all forms of line endings known to man
        final String str = "Robot má udržovat balancované portfolio.\n \r \r\n";
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        if (strategy == Type.SELLING || strategy == Type.RESERVATIONS) {
            assertThat(actualStrategy).isEmpty();
        } else {
            assertThat(actualStrategy).isPresent();
        }
    }

    private static void windows1250WindowsNewlines(final Type strategy) throws IOException {
        // https://github.com/RoboZonky/robozonky/issues/181#issuecomment-346653495
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("newlines-ansi-windows");
        final String str = IOUtils.toString(s, Charset.forName("windows-1250"));
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        if (strategy == Type.SELLING || strategy == Type.RESERVATIONS) {
            assertThat(actualStrategy).isEmpty();
        } else {
            assertThat(actualStrategy).isPresent();
        }
    }

    private static void windows1250UnixNewlines(final Type strategy) throws IOException {
        // https://github.com/RoboZonky/robozonky/issues/181#issuecomment-346653495
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("newlines-ansi-unix");
        final String str = IOUtils.toString(s, Charset.forName("windows-1250"));
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        if (strategy == Type.SELLING || strategy == Type.RESERVATIONS) {
            assertThat(actualStrategy).isEmpty();
        } else {
            assertThat(actualStrategy).isPresent();
        }
    }

    private static void complex(final Type strategy) throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("complex");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        assertThat(actualStrategy).isPresent();
    }

    private static void simplest(final Type strategy) throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("simplest");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        if (strategy == Type.SELLING || strategy == Type.RESERVATIONS) {
            assertThat(actualStrategy).isEmpty();
        } else {
            assertThat(actualStrategy).isPresent();
        }
    }

    private static void disabled(final Type strategy) throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("disabled-filters");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        assertThat(actualStrategy).isEmpty();
    }

    private static void enabled(final Type strategy) throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("enabled-filters");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        if (strategy == Type.SELLING) {
            assertThat(actualStrategy).isEmpty();
        } else {
            assertThat(actualStrategy).isPresent();
        }
    }

    /**
     * This tests a real-life mistake. I forgot to end an expression with EOF - therefore the file was read to the
     * end without error, but whatever was written there was silently ignored. This resulted in an empty strategy,
     * leading the robot to invest into and purchase everything.
     */
    private static void missingHeaders(final Type strategy) throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("no-headers");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        assertThat(actualStrategy).isEmpty();
    }

    private static void missingFilters1(final Type strategy) throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("missing-filters1");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        if (strategy == Type.SELLING || strategy == Type.RESERVATIONS) {
            assertThat(actualStrategy).isEmpty();
        } else {
            assertThat(actualStrategy).isPresent();
        }
    }

    private static void missingFilters2(final Type strategy) throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("missing-filters2");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        if (strategy == Type.RESERVATIONS) {
            assertThat(actualStrategy).isEmpty();
        } else {
            assertThat(actualStrategy).isPresent();
        }
    }

    private static void missingFilters3(final Type strategy) throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("missing-filters3");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        final Optional<?> actualStrategy = getStrategy(strategy, str);
        if (strategy == Type.SELLING || strategy == Type.RESERVATIONS) {
            assertThat(actualStrategy).isEmpty();
        } else {
            assertThat(actualStrategy).isPresent();
        }
    }

    @TestFactory
    Stream<DynamicNode> strategyType() {
        return Stream.of(Type.values())
                .map(type -> dynamicContainer(type.toString(), forType(type)));
    }

    private enum Type {

        INVESTING {
            @Override
            public Function<String, Optional<?>> getStrategy() {
                return SERVICE::toInvest;
            }
        },
        PURCHASING {
            @Override
            public Function<String, Optional<?>> getStrategy() {
                return SERVICE::toPurchase;
            }
        },
        SELLING {
            @Override
            public Function<String, Optional<?>> getStrategy() {
                return SERVICE::toSell;
            }
        },
        RESERVATIONS {
            @Override
            protected Function<String, Optional<?>> getStrategy() {
                return SERVICE::forReservations;
            }
        };

        protected abstract Function<String, Optional<?>> getStrategy();

    }
}

