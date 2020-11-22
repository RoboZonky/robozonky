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
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.OptionalInt;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.Defaults;
import com.github.robozonky.internal.remote.Select;
import com.github.robozonky.internal.test.DateUtil;

/**
 * The purpose of this is that marketplace checks are coupled to information about latest updates to those marketplaces.
 * Each instance of an implementing class may decide to cache or otherwise alter the marketplace during
 * {@link #hasUpdates()} and only ever return that in {@link #getMarketplace()}.
 * 
 * @param <T> Type of the entity coming from the marketplace.
 */
abstract class AbstractMarketplaceAccessor<T> {

    private final Logger logger;
    private final AtomicReference<ZonedDateTime> lastFullMarketplaceCheckReference = new AtomicReference<>(
            Instant.EPOCH.atZone(Defaults.ZONKYCZ_ZONE_ID));

    protected AbstractMarketplaceAccessor(Logger logger) {
        this.logger = logger;
    }

    protected static OptionalInt sanitizeMaximumItemCount(int max) {
        return max >= 0 ? OptionalInt.of(max) : OptionalInt.empty();
    }

    protected Select getIncrementalFilter() {
        var lastFullMarketplaceCheck = lastFullMarketplaceCheckReference.get();
        var now = DateUtil.zonedNow();
        if (lastFullMarketplaceCheck.plus(getForcedMarketplaceCheckInterval())
            .isBefore(now)) {
            var newFullMarketplaceCheck = now.truncatedTo(ChronoUnit.SECONDS); // Go a couple millis back.
            lastFullMarketplaceCheckReference.set(newFullMarketplaceCheck);
            logger.debug(() -> "Running full marketplace check with timestamp of "
                    + DateUtil.toString(newFullMarketplaceCheck)
                    + ", previous was " + DateUtil.toString(lastFullMarketplaceCheck) + ".");
            return getBaseFilter();
        } else {
            var filter = getBaseFilter()
                .greaterThanOrEquals("datePublished", lastFullMarketplaceCheck);
            logger.debug(() -> "Running incremental marketplace check, starting from " +
                    DateUtil.toString(lastFullMarketplaceCheck) + ".");
            return filter;
        }
    }

    protected abstract OptionalInt getMaximumItemsToRead();

    protected abstract Select getBaseFilter();

    public abstract Duration getForcedMarketplaceCheckInterval();

    public abstract Stream<T> getMarketplace();

    public abstract boolean hasUpdates();

}
