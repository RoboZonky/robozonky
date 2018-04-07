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

package com.github.robozonky.app.portfolio;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.internal.api.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Repayments implements PortfolioDependant {

    private static final Logger LOGGER = LoggerFactory.getLogger(Repayments.class);

    private static final State.ClassSpecificState STATE = State.forClass(Repayments.class);
    private static final String STATE_KEY = "lastChecked", LAST_UPDATE_PROPERTY_NAME = "lastUpdate";

    private static Set<Integer> getRepaidLastTime() {
        return STATE.getValues(STATE_KEY)
                .orElse(Collections.emptyList())
                .stream()
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    private static boolean isInitialized() {
        return STATE.getValue(LAST_UPDATE_PROPERTY_NAME).isPresent();
    }

    private static void setRepaid(final Collection<Integer> ids) {
        final Stream<String> toStore = ids.stream().map(String::valueOf).sorted();
        STATE.newBatch()
                .set(STATE_KEY, toStore)
                .set(LAST_UPDATE_PROPERTY_NAME, OffsetDateTime.now().toString())
                .call();
    }

    @Override
    public void accept(final Portfolio portfolio, final Authenticated authenticated) {
        final Select s = new Select()
                .in("loan.status", "PAID")
                .equals("status", "ACTIVE"); // this is how Zonky queries for loans fully repaid
        final Collection<Investment> repaid = authenticated.call(z -> z.getInvestments(s)).collect(Collectors.toList());
        if (isInitialized()) { // detect and process loans that have been fully repaid, comparing to the last time
            final Collection<Integer> repaidLastTime = getRepaidLastTime();
            LOGGER.trace("State: {}.", repaidLastTime);
            repaid.stream()
                    .filter(i -> repaidLastTime.stream().noneMatch(loanId -> loanId != i.getLoanId()))
                    .forEach(i -> {
                        final Loan l = authenticated.call(zonky -> LoanCache.INSTANCE.getLoan(i, zonky));
                        final PortfolioOverview portfolioOverview = portfolio.calculateOverview();
                        final Event e = new LoanRepaidEvent(i, l, portfolioOverview);
                        Events.fire(e);
                    });
        }
        // store for future reference
        setRepaid(repaid.stream().map(Investment::getLoanId).collect(Collectors.toSet()));
    }
}
