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

package com.github.triceo.robozonky.strategy.natural;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;

import com.github.triceo.robozonky.api.strategies.StrategyService;
import com.github.triceo.robozonky.internal.api.Defaults;
import org.apache.commons.io.IOUtils;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class NaturalLanguageStrategyServiceTest {

    @Parameterized.Parameter
    public Function<String, Optional<Object>> strategyProvider;

    private static Function<String, Optional<?>> getInvesting(final StrategyService s) {
        return s::toInvest;
    }

    private static Function<String, Optional<?>> getPurchasing(final StrategyService s) {
        return s::toPurchase;
    }

    private static Function<String, Optional<?>> getSelling(final StrategyService s) {
        return s::toSell;
    }

    @Parameterized.Parameters
    public static Collection<Object[]> parameters() {
        final Collection<Object[]> result = new ArrayList<>();
        final StrategyService service = new NaturalLanguageStrategyService();
        result.add(new Object[]{getInvesting(service)});
        result.add(new Object[]{getPurchasing(service)});
        result.add(new Object[]{getSelling(service)});
        return result;
    }

    @Test
    public void test() throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("only-whitespace");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        Assertions.assertThat(strategyProvider.apply(str)).isEmpty();
    }

    @Test
    public void complex() throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("complex");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        Assertions.assertThat(strategyProvider.apply(str)).isPresent();
    }

    @Test
    public void simplest() throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("simplest");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        Assertions.assertThat(strategyProvider.apply(str)).isPresent();
    }

    /**
     * This tests a real-life mistake. I forgot to end an expression with EOF - therefore the file was read to the
     * end without error, but whatever was written there was silently ignored. This resulted in an empty strategy,
     * leading the robot to invest into and purchase everything.
     */
    @Test
    public void missingHeaders() throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("no-headers");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        Assertions.assertThat(strategyProvider.apply(str)).isEmpty();
    }

    @Test
    public void missingFilters1() throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("missing-filters1");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        Assertions.assertThat(strategyProvider.apply(str)).isPresent();
    }

    @Test
    public void missingFilters2() throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("missing-filters2");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        Assertions.assertThat(strategyProvider.apply(str)).isPresent();
    }

    @Test
    public void missingFilters3() throws IOException {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("missing-filters3");
        final String str = IOUtils.toString(s, Defaults.CHARSET);
        Assertions.assertThat(strategyProvider.apply(str)).isPresent();
    }
}

