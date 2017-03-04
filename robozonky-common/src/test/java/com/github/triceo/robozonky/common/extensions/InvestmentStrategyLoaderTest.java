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

package com.github.triceo.robozonky.common.extensions;

import java.util.Optional;
import java.util.UUID;

import com.github.triceo.robozonky.api.strategies.InvestmentStrategy;
import com.github.triceo.robozonky.api.strategies.InvestmentStrategyService;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class InvestmentStrategyLoaderTest {

    @Test
    public void unknown() {
        final Optional<InvestmentStrategy> result = InvestmentStrategyLoader.load(UUID.randomUUID().toString());
        Assertions.assertThat(result).isEmpty();
    }

    @Test
    public void failedProcessing() {
        final InvestmentStrategyService iss = Mockito.mock(InvestmentStrategyService.class);
        Mockito.doThrow(new IllegalStateException("Testing")).when(iss).parse(ArgumentMatchers.any());
        Assertions.assertThat(InvestmentStrategyLoader.processInvestmentStrategyService(iss, "")).isEmpty();
    }

    @Test
    public void standardProcessing() {
        final InvestmentStrategyService iss = Mockito.mock(InvestmentStrategyService.class);
        Mockito.when(iss.parse(ArgumentMatchers.any())).thenReturn(Optional.of(Mockito.mock(InvestmentStrategy.class)));
        Assertions.assertThat(InvestmentStrategyLoader.processInvestmentStrategyService(iss, "")).isPresent();
    }

}
