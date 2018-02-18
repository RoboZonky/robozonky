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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.notifications.Event;
import com.github.robozonky.api.notifications.LoanRepaidEvent;
import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.remote.entities.sanitized.Loan;
import com.github.robozonky.api.remote.enums.PaymentStatus;
import com.github.robozonky.api.remote.enums.PaymentStatuses;
import com.github.robozonky.api.strategies.PortfolioOverview;
import com.github.robozonky.app.Events;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.util.LoanCache;
import com.github.robozonky.internal.api.State;

public class Repayments implements PortfolioDependant {

    private static final State.ClassSpecificState STATE = State.forClass(Repayments.class);
    private static final String STATE_KEY = "activeLastTime";

    private final boolean isDryRun;

    public Repayments(final boolean isDryRun) {
        this.isDryRun = isDryRun;
    }

    private static Set<Integer> getActiveLastTime() {
        return STATE.getValues(STATE_KEY)
                .orElse(Collections.emptyList())
                .stream()
                .map(Integer::parseInt)
                .collect(Collectors.toSet());
    }

    private static void setActive(final Collection<Integer> ids) {
        final Stream<String> toStore = ids.stream().map(String::valueOf).sorted();
        STATE.newBatch().set(STATE_KEY, toStore).call();
    }

    @Override
    public void accept(final Portfolio portfolio, final Authenticated authenticated) {
        final PortfolioOverview portfolioOverview =
                authenticated.call(zonky -> portfolio.calculateOverview(zonky, isDryRun));
        final Collection<Integer> active = getActiveLastTime();
        // detect and process loans that have been fully repaid, comparing to the last time active loans were checked
        final Stream<Investment> repaid =
                portfolio.getActiveWithPaymentStatus(PaymentStatuses.of(PaymentStatus.PAID));
        repaid.filter(i -> active.contains(i.getLoanId()))
                .peek(i -> {
                    final Loan l = authenticated.call(zonky -> LoanCache.INSTANCE.getLoan(i.getLoanId(), zonky));
                    final Event e = new LoanRepaidEvent(i, l, portfolioOverview);
                    Events.fire(e);
                })
                .forEach(i -> active.remove(i.getLoanId()));
        // add all active loans to date
        portfolio.getActiveWithPaymentStatus(PaymentStatus.getActive())
                .forEach(i -> active.add(i.getLoanId()));
        // store for future reference
        setActive(active);
    }
}
