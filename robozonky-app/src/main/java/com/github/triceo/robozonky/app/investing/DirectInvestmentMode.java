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

package com.github.triceo.robozonky.app.investing;

import java.util.Collection;
import java.util.Collections;
import java.util.function.Function;

import com.github.triceo.robozonky.api.ReturnCode;
import com.github.triceo.robozonky.api.remote.entities.Investment;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.strategies.LoanDescriptor;
import com.github.triceo.robozonky.app.authentication.Authenticated;
import com.github.triceo.robozonky.app.configuration.InvestmentMode;
import com.github.triceo.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DirectInvestmentMode implements InvestmentMode {

    private static final Logger LOGGER = LoggerFactory.getLogger(DirectInvestmentMode.class);

    private final int loanId, loanAmount;
    private final Authenticated authenticated;
    private final Investor.Builder builder;
    private final boolean isFaultTolerant;

    public DirectInvestmentMode(final Authenticated auth, final Investor.Builder builder, final boolean isFaultTolerant,
                                final int loanId, final int loanAmount) {
        this.authenticated = auth;
        this.builder = builder;
        this.isFaultTolerant = isFaultTolerant;
        this.loanId = loanId;
        this.loanAmount = loanAmount;
    }

    @Override
    public ReturnCode get() {
        LOGGER.trace("Executing.");
        try {
            final Function<Zonky, Collection<Investment>> op = (zonky) -> {
                final Loan l = zonky.getLoan(loanId);
                final LoanDescriptor d = new LoanDescriptor(l);
                return d.recommend(loanAmount, false)
                        .map(r -> Session.invest(builder, zonky, new DirectInvestmentCommand(r)))
                        .orElse(Collections.emptyList());
            };
            authenticated.call(op);
            return ReturnCode.OK;
        } catch (final Exception ex) {
            LOGGER.error("Failed executing investments.", ex);
            return ReturnCode.ERROR_UNEXPECTED;
        }
    }

    @Override
    public boolean isFaultTolerant() {
        return isFaultTolerant;
    }

    @Override
    public boolean isDryRun() {
        return builder.isDryRun();
    }

    @Override
    public String getUsername() {
        return authenticated.getSecretProvider().getUsername();
    }
}
