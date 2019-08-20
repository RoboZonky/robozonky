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

package com.github.robozonky.app.tenant;

import java.time.Duration;
import java.util.SortedSet;
import java.util.TreeSet;

import com.github.robozonky.internal.remote.RequestCounter;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class TooManyRequestWarningSystem implements Runnable {

    private static final Logger LOGGER = LogManager.getLogger(TooManyRequestWarningSystem.class);
    private static final int[] THRESHOLDS_IN_PERCENT = new int[] {50, 75, 90, 99};
    private static final int LIMIT_PER_TEN_MINUTES = 600;
    private static final Duration TEN_MINUTES = Duration.ofMinutes(10);
    private static final Duration ONE_HOUR = Duration.ofHours(1);

    private final RequestCounter counter;
    private final SortedSet<Integer> thresholdsReached = new TreeSet<>();


    public TooManyRequestWarningSystem(final RequestCounter counter) {
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
        final int requestsInLast600Seconds = counter.count(TEN_MINUTES);
        boolean informedOfReturnToNormal = false;
        for (int i = 0; i < THRESHOLDS_IN_PERCENT.length; i++) {
            final int thresholdInPercent = THRESHOLDS_IN_PERCENT[i];
            final int actualThreshold = (LIMIT_PER_TEN_MINUTES * thresholdInPercent) / 100;
            if (requestsInLast600Seconds < actualThreshold) { // OK
                if (thresholdsReached.contains(thresholdInPercent)) { // was not OK before, inform
                    thresholdsReached.remove(thresholdInPercent);
                    if (!informedOfReturnToNormal) {
                        LOGGER.info("Request quota back below {} % ({}).", thresholdInPercent, requestsInLast600Seconds);
                        informedOfReturnToNormal = true;
                    }
                } else {
                    // no change
                }
            } else { // not OK
                if (thresholdsReached.contains(thresholdInPercent)) {
                    // no change
                } else { // newly over threshold, inform
                    thresholdsReached.add(thresholdInPercent);
                    if (thresholdInPercent >= 90) {
                        LOGGER.warn("Request quota over {} % ({}), throttling imminent.", thresholdInPercent,
                                    requestsInLast600Seconds);
                    } else {
                        LOGGER.info("Request quota over {} % ({}).", thresholdInPercent, requestsInLast600Seconds);
                    }
                }
            }
        }
        counter.keepOnly(ONE_HOUR); // clean up a bit
    }
}
