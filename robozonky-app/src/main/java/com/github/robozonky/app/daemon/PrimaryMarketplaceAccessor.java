/*
 * Copyright 2019 The RoboZonky Project
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

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.common.remote.Select;
import com.github.robozonky.common.tenant.Tenant;
import com.github.robozonky.internal.util.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class PrimaryMarketplaceAccessor implements MarketplaceAccessor<LoanDescriptor> {

    private static final Logger LOGGER = LogManager.getLogger();
    /**
     * Will make sure that the endpoint only loads loans that are on the marketplace, and not the entire history.
     */
    private static final Select SELECT = new Select().greaterThan("nonReservedRemainingInvestment", 0);
    private final Tenant tenant;
    private final Function<long[], long[]> lastChecked;
    private final AtomicReference<Collection<LoanDescriptor>> marketplace = new AtomicReference<>();

    public PrimaryMarketplaceAccessor(final Tenant tenant, final Function<long[], long[]> lastCheckedSetter) {
        this.tenant = tenant;
        this.lastChecked = lastCheckedSetter;
    }

    private static boolean contains(final long toFind, final long... original) {
        for (final long j : original) {
            if (j == toFind) {
                return true;
            }
        }
        return false;
    }

    static boolean hasAdditions(final long[] current, final long... original) {
        if (current.length == 0) {
            return false;
        } else if (current.length > original.length) {
            return true;
        }
        for (final long i : current) {
            final boolean found = contains(i, original);
            if (!found) {
                return true;
            }
        }
        return false;
    }

    private static boolean isActionable(final LoanDescriptor loanDescriptor) {
        final OffsetDateTime now = DateUtil.offsetNow();
        return loanDescriptor.getLoanCaptchaProtectionEndDateTime()
                .map(d -> d.isBefore(now))
                .orElse(true);
    }

    /**
     * In order to not have to run the strategy over a marketplace and save CPU cycles, we need to know if the
     * marketplace changed since the last time this method was called.
     * @param marketplace Present contents of the marketplace.
     * @return Returning true triggers evaluation of the strategy.
     */
    private boolean hasMarketplaceUpdates(final Collection<LoanDescriptor> marketplace) {
        final long[] idsFromMarketplace = marketplace.stream().mapToLong(p -> p.item().getId()).toArray();
        final long[] presentWhenLastChecked = lastChecked.apply(idsFromMarketplace);
        return hasAdditions(idsFromMarketplace, presentWhenLastChecked);
    }

    private Stream<LoanDescriptor> readMarketplace() {
        return tenant.call(zonky -> zonky.getAvailableLoans(SELECT))
                .filter(l -> !l.getMyInvestment().isPresent()) // re-investing would fail
                .map(LoanDescriptor::new)
                .filter(PrimaryMarketplaceAccessor::isActionable);
    }

    @Override
    public Collection<LoanDescriptor> getMarketplace() {
        return marketplace.updateAndGet(old -> {
            if (old != null) {
                return old;
            }
            return readMarketplace().collect(Collectors.toList());
        });
    }

    @Override
    public boolean hasUpdates() {
        return hasMarketplaceUpdates(getMarketplace());
    }
}
