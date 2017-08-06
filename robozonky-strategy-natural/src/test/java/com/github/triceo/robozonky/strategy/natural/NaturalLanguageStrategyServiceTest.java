/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.strategy.natural;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import com.github.triceo.robozonky.api.strategies.StrategyService;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class NaturalLanguageStrategyServiceTest {

    private static Function<InputStream, Optional<?>> getInvesting(final StrategyService s) {
        return s::toInvest;
    }

    private static Function<InputStream, Optional<?>> getPurchasing(final StrategyService s) {
        return s::toPurchase;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        final Collection<Object[]> result = new ArrayList<>();
        final StrategyService service = new NaturalLanguageStrategyService();
        result.add(new Object[]{getInvesting(service)});
        result.add(new Object[]{getPurchasing(service)});
        return result;
    }

    @Parameterized.Parameter
    public Function<InputStream, Optional<Object>> strategyProvider;

    @Test
    public void test() {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("only-whitespace");
        Assertions.assertThat(strategyProvider.apply(s)).isEmpty();
    }

    @Test
    public void complex() {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("complex");
        Assertions.assertThat(strategyProvider.apply(s)).isPresent();
    }

    @Test
    public void simplest() {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("simplest");
        Assertions.assertThat(strategyProvider.apply(s)).isPresent();
    }
}
