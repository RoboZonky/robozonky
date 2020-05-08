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

package com.github.robozonky.internal.remote;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.test.DateUtil;

final class RequestCounterImpl implements RequestCounter {

    private static final Logger LOGGER = LogManager.getLogger(RequestCounterImpl.class);

    private SortedSet<Instant> requests = new ConcurrentSkipListSet<>();

    @Override
    public void mark() {
        requests.add(DateUtil.now());
    }

    @Override
    public boolean hasMoreRecent(final Instant request) {
        var thisRequestOrNewer = requests.tailSet(request);
        if (thisRequestOrNewer.isEmpty()) {
            return false;
        } else if (thisRequestOrNewer.size() == 1) {
            return !thisRequestOrNewer.contains(request);
        } else {
            return true;
        }
    }

    @Override
    public int count() {
        return count(Duration.ZERO);
    }

    @Override
    public int count(final Duration interval) {
        if (Objects.equals(interval, Duration.ZERO)) {
            return requests.size();
        }
        var threshold = DateUtil.now()
            .minus(interval);
        return requests.tailSet(threshold)
            .size();
    }

    @Override
    public void cut(final int count) {
        requests.stream()
            .limit(count)
            .forEach(requests::remove);
        LOGGER.trace("Removed {} request(s), new total is {}.", count, count());
    }

    @Override
    public void keepOnly(final Duration interval) {
        LOGGER.trace("Resetting to within the last interval: {}.", interval);
        var threshold = DateUtil.now()
            .minus(interval);
        requests = requests.tailSet(threshold);
    }
}
