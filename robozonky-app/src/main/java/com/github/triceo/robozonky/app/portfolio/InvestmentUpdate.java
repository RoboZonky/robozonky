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

package com.github.triceo.robozonky.app.portfolio;

import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.app.util.DaemonRuntimeExceptionHandler;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class InvestmentUpdate implements Consumer<Zonky> {

    private static final Logger LOGGER = LoggerFactory.getLogger(InvestmentUpdate.class);

    @Override
    public void accept(final Zonky zonky) {
        try {
            LOGGER.info("Daily update started.");
            final List<Investment> investments = zonky.getInvestments().collect(Collectors.toList());
            Investments.INSTANCE.update(zonky, investments);
            LOGGER.debug("Finished.");
        } catch (final Throwable t) {
            new DaemonRuntimeExceptionHandler().handle(t);
        }
    }
}
