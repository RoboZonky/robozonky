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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.util.Arrays;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.remote.ApiProvider;
import com.github.robozonky.internal.remote.RequestCounter;

/**
 * Zonky implements a quota for how many operations a user can perform over their API during a given period of time.
 * This class measures a {@link RequestCounter} (possibly coming from {@link ApiProvider}) and issues warnings on the
 * use of that quota.
 */
final class QuotaMonitor implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(QuotaMonitor.class);
    private static final int[] THRESHOLDS_IN_PERCENT = new int[] { 50, 75, 90, 99 };
    private static final int LIMIT_PER_THROTTLING_INTERVAL = 3000;
    private static final Duration THROTTLING_INTERVAL = Duration.ofSeconds(3000);

    private final RequestCounter counter;
    private final SortedSet<Integer> thresholdsReached = new TreeSet<>();

    public QuotaMonitor(final RequestCounter counter) {
        this.counter = counter;
    }

    public boolean threshold50PercentReached() {
        return thresholdReached(50);
    }

    public boolean threshold75PercentReached() {
        return thresholdReached(75);
    }

    public boolean threshold90PercentReached() {
        return thresholdReached(90);
    }

    public boolean threshold99PercentReached() {
        return thresholdReached(99);
    }

    private boolean thresholdReached(final int percentage) {
        return thresholdsReached.contains(percentage);
    }

    @Override
    public void run() {
        final int requestsInLast600Seconds = counter.count(THROTTLING_INTERVAL);
        final AtomicBoolean informedOfReturnToNormal = new AtomicBoolean();
        LOGGER.debug("Requests in {}: {}.", THROTTLING_INTERVAL, requestsInLast600Seconds);
        Arrays.stream(THRESHOLDS_IN_PERCENT)
            .forEach(thresholdInPercent -> {
                final double actualThreshold = (LIMIT_PER_THROTTLING_INTERVAL * thresholdInPercent) / 100.0;
                final boolean thresholdReachedBefore = thresholdReached(thresholdInPercent);
                if (requestsInLast600Seconds < actualThreshold) { // OK
                    if (thresholdReachedBefore) { // was not OK before, inform
                        thresholdsReached.remove(thresholdInPercent);
                        if (informedOfReturnToNormal.getAndSet(true)) {
                            // thresholds processed from low to high; once lower done, no need to inform of any higher
                            LOGGER.info("Request quota estimated back below {} % ({}).", thresholdInPercent,
                                    requestsInLast600Seconds);
                        }
                    }
                } else { // not OK
                    if (!thresholdReachedBefore) { // newly over threshold, inform
                        thresholdsReached.add(thresholdInPercent);
                        if (thresholdInPercent > 90) {
                            LOGGER.warn("Request quota estimated over {} % ({}), throttling imminent.",
                                    thresholdInPercent,
                                    requestsInLast600Seconds);
                        } else {
                            LOGGER.info("Request quota estimated over {} % ({}).", thresholdInPercent,
                                    requestsInLast600Seconds);
                        }
                    }
                }
            });
        counter.keepOnly(THROTTLING_INTERVAL.multipliedBy(2)); // clean up a bit
    }
}
