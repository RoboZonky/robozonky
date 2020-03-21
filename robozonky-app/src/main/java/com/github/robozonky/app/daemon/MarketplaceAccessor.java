/*
 * Copyright 2020 The RoboZonky Project
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
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.test.DateUtil;
import org.apache.logging.log4j.Logger;

/**
 * The purpose of implementations of this interface is that marketplace checks are coupled to information about latest
 * updates to those marketplaces. Each instance of an implementing class may decide to cache the marketplace during
 * {@link #hasUpdates()} and only ever return that in {@link #getMarketplace()}.
 * @param <T> Type of the entity coming from the marketplace.
 */
abstract class MarketplaceAccessor<T> {

    protected Select makeIncremental(final Select originalFilter,
                                     final AtomicReference<Instant> lastFullMarketplaceCheckReference) {
        Instant lastFullMarketplaceCheck = lastFullMarketplaceCheckReference.get();
        Instant now = DateUtil.now();
        if (lastFullMarketplaceCheck.plus(getForcedMarketplaceCheckInterval()).isBefore(now)) {
            lastFullMarketplaceCheckReference.set(now);
            getLogger().debug("Running full marketplace check with timestamp of {}, previous was {}.", now,
                              lastFullMarketplaceCheck);
            return originalFilter;
        } else {
            var filter = originalFilter.greaterThanOrEquals("datePublished",
                                                OffsetDateTime.ofInstant(lastFullMarketplaceCheck, Defaults.ZONE_ID));
            getLogger().debug("Running incremental marketplace check, starting from {}.", lastFullMarketplaceCheck);
            return filter;
        }
    }

    public abstract Duration getForcedMarketplaceCheckInterval();

    public abstract Collection<T> getMarketplace();

    public abstract boolean hasUpdates();

    protected abstract Logger getLogger();

}
