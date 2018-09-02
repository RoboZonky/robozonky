/*
 * Copyright 2018 The RoboZonky Project
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
import ch.qos.logback.core.spi.ContextAwareBase;

/**
 * This is a last-resort logging system to load logback.xml from resources. This is required in CLI and in Installer,
 * which are distributed as plain JARs and therefore there is a need to provide detailed default logging to help with
 * user-reported problems.
 * <p>
 * The code used here comes from https://logback.qos.ch/manual/configuration.html, section "Invoking Joran Directly".
 */
public final class LogbackConfigurator extends ContextAwareBase implements Configurator {

    private final GenericConfigurator configurator;

    public LogbackConfigurator() {
        this(new JoranConfigurator());
    }

    LogbackConfigurator(final GenericConfigurator configurator) {
        this.configurator = configurator;
    }

    @Override
    public void configure(final LoggerContext loggerContext) {
        try {
            configurator.setContext(loggerContext);
            loggerContext.reset();
            configurator.doConfigure(getClass().getResource("/logback.xml"));
        } catch (final Exception ex) { // this is a critical error, no debugging == no means of helping users
            throw new IllegalStateException("Failed reading Logback configuration.", ex);
        }
    }
}
