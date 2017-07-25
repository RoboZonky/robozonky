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

package com.github.triceo.robozonky.app.configuration;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.beust.jcommander.Parameters;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.investing.InvestmentMode;
import com.github.triceo.robozonky.app.investing.Investor;
import com.github.triceo.robozonky.common.extensions.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(commandNames = "test", commandDescription = "Check that e-mails and integrations work properly.")
class TestOperatingMode extends OperatingMode {

    @Override
    protected Optional<InvestmentMode> getInvestmentMode(final CommandLine cli, final Authenticated auth,
                                                         final Investor.Builder builder) {
        return Optional.of(new InvestmentMode() {

            private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

            @Override
            public Optional<Collection<Investment>> get() {
                LOGGER.info("Notification sent: {}.", Checker.notifications(auth.getSecretProvider().getUsername()));
                builder.getConfirmationUsed().ifPresent(c ->
                                                                builder.getConfirmationRequestUsed().ifPresent(r ->
                                                                                                                       LOGGER.info(
                                                                                                                               "Confirmation received: {}.",
                                                                                                                               Checker.confirmations(
                                                                                                                                       c,
                                                                                                                                       r.getUserId(),
                                                                                                                                       r.getPassword()))
                                                                )
                );
                return Optional.of(Collections.emptyList());
            }
        });
    }
}
