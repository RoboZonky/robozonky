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

package com.github.robozonky.app.daemon;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.authentication.EventTenant;
import com.github.robozonky.app.daemon.operations.Investing;
import com.github.robozonky.app.daemon.operations.Investor;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.DateUtil;

class InvestingDaemon extends DaemonOperation {

    /**
     * Will make sure that the endpoint only loads loans that are on the marketplace, and not the entire history.
     */
    private static final Select SELECT = new Select().greaterThan("remainingInvestment", 0);
    private final Investing investing;

    public InvestingDaemon(final Consumer<Throwable> shutdownCall, final EventTenant auth, final Investor investor,
                           final Duration refreshPeriod) {
        super(shutdownCall, auth, refreshPeriod);
        this.investing = new Investing(investor, auth);
    }

    private static boolean isActionable(final LoanDescriptor loanDescriptor) {
        final OffsetDateTime now = DateUtil.offsetNow();
        return loanDescriptor.getLoanCaptchaProtectionEndDateTime()
                .map(d -> d.isBefore(now))
                .orElse(true);
    }

    @Override
    protected boolean isEnabled(final Tenant authenticated) {
        return !authenticated.getRestrictions().isCannotInvest();
    }

    @Override
    protected void execute(final Tenant authenticated) {
        // don't query anything unless we have enough money to invest
        final long balance = authenticated.getPortfolio().getBalance().longValue();
        final int minimum = authenticated.getRestrictions().getMinimumInvestmentAmount();
        if (balance < minimum) {
            LOGGER.debug("Asleep as there is not enough available balance. ({} < {})", balance, minimum);
            return;
        }
        // query marketplace for investment opportunities
        final Collection<LoanDescriptor> loans = authenticated.call(zonky -> zonky.getAvailableLoans(SELECT))
                .filter(l -> !l.getMyInvestment().isPresent()) // re-investing would fail
                .map(LoanDescriptor::new)
                .filter(InvestingDaemon::isActionable)
                .collect(Collectors.toList());
        investing.apply(loans);
    }
}
