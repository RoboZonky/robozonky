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

import com.github.triceo.robozonky.api.strategies.StrategyService;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class NaturalLanguageStrategyServiceTest {

    @Test
    public void test() {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("only-whitespace");
        final StrategyService service = new NaturalLanguageStrategyService();
        Assertions.assertThat(service.toInvest(s)).isEmpty();
    }

    @Test
    public void complex() {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("complex");
        final StrategyService service = new NaturalLanguageStrategyService();
        Assertions.assertThat(service.toInvest(s)).isPresent();
    }

    @Test
    public void simplest() {
        final InputStream s = NaturalLanguageStrategyServiceTest.class.getResourceAsStream("simplest");
        final StrategyService service = new NaturalLanguageStrategyService();
        Assertions.assertThat(service.toInvest(s)).isPresent();
    }
}
