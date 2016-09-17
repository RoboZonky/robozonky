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

import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

import com.github.triceo.robozonky.strategy.InvestmentStrategy;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.Mockito;

@RunWith(Parameterized.class)
public class MinimumMaximumTest {

    @Parameterized.Parameters(name = "{0}")
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(
                new Object[] {(Supplier<String>) StrategyFileProperty.MAXIMUM_INVESTMENT::getKey,
                        (Function<ImmutableConfiguration, Integer>) SimpleInvestmentStrategyService::getMaximumInvestment},
                new Object[] {(Supplier<String>) StrategyFileProperty.MINIMUM_BALANCE::getKey,
                        (Function<ImmutableConfiguration, Integer>) SimpleInvestmentStrategyService::getMinimumBalance}
        );
    }

    // this weird implementation exists because if we supplied value directly, it would not count towards PIT mutations
    @Parameterized.Parameter
    public Supplier<String> keyRetrieval;

    @Parameterized.Parameter(1)
    public Function<ImmutableConfiguration, Integer> valueRetrieval;

    @Test(expected = IllegalStateException.class)
    public void nonexistent() {
        final String propertyName = keyRetrieval.get();
        final ImmutableConfiguration config = Mockito.mock(ImmutableConfiguration.class);
        Mockito.when(config.containsKey(propertyName)).thenReturn(false);
        valueRetrieval.apply(config);
    }

    @Test(expected = IllegalStateException.class)
    public void outOfBounds() {
        final String propertyName = keyRetrieval.get();
        final ImmutableConfiguration config = Mockito.mock(ImmutableConfiguration.class);
        Mockito.when(config.containsKey(propertyName)).thenReturn(true);
        Mockito.when(config.getInt(propertyName))
                .thenReturn(Optional.of(InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED - 1));
        valueRetrieval.apply(config);
    }

    @Test
    public void proper() {
        final String propertyName = keyRetrieval.get();
        final int result = InvestmentStrategy.MINIMAL_INVESTMENT_ALLOWED;
        final ImmutableConfiguration config = Mockito.mock(ImmutableConfiguration.class);
        Mockito.when(config.containsKey(propertyName)).thenReturn(true);
        Mockito.when(config.getInt(propertyName)).thenReturn(Optional.of(result));
        Assertions.assertThat(valueRetrieval.apply(config)).isEqualTo(result);
    }

}
