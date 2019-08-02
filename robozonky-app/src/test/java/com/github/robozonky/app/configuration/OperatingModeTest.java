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

package com.github.robozonky.app.configuration;

import java.util.function.Supplier;

import com.github.robozonky.app.AbstractZonkyLeveragingTest;
import com.github.robozonky.internal.secrets.SecretProvider;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class OperatingModeTest extends AbstractZonkyLeveragingTest {

    @Test
    void basic() {
        final CommandLine cli = mock(CommandLine.class);
        when(cli.getStrategyLocation()).thenReturn("");
        final SecretProvider secretProvider = SecretProvider.inMemory("user", new char[0]);
        final OperatingMode mode = new OperatingMode(mock(Supplier.class));
        final InvestmentMode config = mode.configure(cli, secretProvider);
        assertThat(config).isNotNull();
    }
}
