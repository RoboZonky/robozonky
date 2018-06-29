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

package com.github.robozonky.app.configuration;

import java.util.Optional;

import com.beust.jcommander.Parameters;
import com.github.robozonky.app.ReturnCode;
import com.github.robozonky.app.authentication.Tenant;
import com.github.robozonky.app.investing.Investor;
import com.github.robozonky.app.runtime.Lifecycle;
import com.github.robozonky.common.extensions.Checker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Parameters(commandNames = "test", commandDescription = "Check that e-mails and integrations work properly.")
class TestOperatingMode extends OperatingMode {

    @Override
    protected Optional<InvestmentMode> getInvestmentMode(final CommandLine cli, final Tenant auth,
                                                         final Investor investor) {
        return Optional.of(new InvestmentMode() {

            private final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

            @Override
            public ReturnCode apply(final Lifecycle lifecycle) {
                cli.getNotificationConfigLocation().ifPresent(cfg -> {
                    final boolean sent = Checker.notifications(auth.getSessionInfo().getUsername(), cfg);
                    LOGGER.info("Notification sent: {}.", sent);
                });
                return investor.getConfirmationProvider().map(c -> investor.getRequestId()
                        .map(r -> {
                            LOGGER.info("Confirmation received: {}.",
                                        Checker.confirmations(c, r.getUserId(), r.getPassword()));
                            return ReturnCode.OK;
                        }).orElse(ReturnCode.ERROR_UNEXPECTED))
                        .orElse(ReturnCode.OK);
            }
        });
    }
}
