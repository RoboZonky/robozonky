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

package com.github.robozonky.cli;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.classic.spi.Configurator;
import ch.qos.logback.core.joran.GenericConfigurator;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LogbackConfiguratorTest {

    @Test
    void parsesLogback() {
        final GenericConfigurator j = spy(new JoranConfigurator());
        final LoggerContext l = (LoggerContext) LoggerFactory.getILoggerFactory();
        final LoggerContext spied = spy(l);
        final Configurator c = new LogbackConfigurator(j);
        c.configure(spied);
        verify(spied).reset();
        verify(j).setContext(eq(spied));
    }

    @Test
    void failsProperly() {
        final GenericConfigurator j = mock(GenericConfigurator.class);
        final LoggerContext l = (LoggerContext) LoggerFactory.getILoggerFactory();
        final LoggerContext spied = spy(l);
        final Configurator c = new LogbackConfigurator(j);
        assertThatThrownBy(() -> c.configure(spied)).isInstanceOf(IllegalStateException.class);
    }

}
