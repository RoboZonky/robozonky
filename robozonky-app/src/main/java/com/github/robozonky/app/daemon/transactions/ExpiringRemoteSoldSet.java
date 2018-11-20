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

package com.github.robozonky.app.daemon.transactions;

import java.time.Duration;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.common.Tenant;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.util.Expiring;

final class ExpiringRemoteSoldSet extends Expiring<Set<Integer>> {

    private final Supplier<Set<Integer>> refresher;

    public ExpiringRemoteSoldSet(final Tenant tenant) {
        super(Duration.ofMinutes(5));
        this.refresher = () -> getSoldLoans(tenant);
    }

    private static Set<Integer> getSoldLoans(final Tenant tenant) {
        final Select s = new Select().equals("status", "SOLD");
        return tenant.call(zonky -> zonky.getInvestments(s))
                .mapToInt(Investment::getLoanId)
                .distinct()
                .boxed()
                .collect(Collectors.toSet());
    }

    @Override
    protected Optional<Set<Integer>> retrieve() {
        try {
            return Optional.ofNullable(refresher.get());
        } catch (final Exception ex) {
            LOGGER.info("Failed fetching the list of sold loans from Zonky. Using last known one.", ex);
            return Optional.empty();
        }
    }
}
