/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.app;

import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import com.github.triceo.robozonky.remote.Api;
import com.github.triceo.robozonky.remote.Loan;

class Marketplace {

    public static Marketplace from(final Api api) {
        return new Marketplace(api.getLoans());
    }

    private final List<Loan> recentLoansDescending;

    private Marketplace(final Collection<Loan> loans) {
        this.recentLoansDescending = Collections.unmodifiableList(loans.stream()
                .filter(l -> l.getRemainingInvestment() > 0)
                .sorted(Comparator.comparing(Loan::getDatePublished).reversed())
                .collect(Collectors.toList()));
    }

    public List<Loan> getAllLoans() {
        return this.recentLoansDescending;
    }

    public List<Loan> getLoansOlderThan(final int delayInSeconds) {
        return this.recentLoansDescending.stream()
                .filter(l -> Instant.now().isAfter(l.getDatePublished().plus(delayInSeconds, ChronoUnit.SECONDS)))
                .collect(Collectors.toList());
    }

    public List<Loan> getLoansNewerThan(final Instant instant) {
        return this.recentLoansDescending.stream()
                .filter(l -> l.getDatePublished().isAfter(instant))
                .collect(Collectors.toList());
    }

    public MarketplaceView newView(final AppContext ctx, final Path state) {
        return new MarketplaceView(ctx, this, state);
    }

}
