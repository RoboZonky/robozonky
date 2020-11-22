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
import java.time.ZonedDateTime;
import java.util.SortedSet;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.stream.Stream;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.github.robozonky.internal.test.DateUtil;

final class RequestCounterImpl implements RequestCounter {

    private static final Logger LOGGER = LogManager.getLogger(RequestCounterImpl.class);

    private SortedSet<ZonedDateTime> requests = new ConcurrentSkipListSet<>();

    @Override
    public void mark() {
        requests.add(DateUtil.zonedNow());
    }

    @Override
    public boolean hasMoreRecent(final ZonedDateTime request) {
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
        return requests.size();
    }

    @Override
    public int count(final Duration interval) {
        var threshold = DateUtil.zonedNow()
            .minus(interval);
        return requests.tailSet(threshold)
            .size();
    }

    @Override
    public void cut(final int count) {
        LOGGER.trace("Requesting to remove {} requests.", count);
        var removed = requests.stream()
            .flatMap(i -> requests.remove(i) ? Stream.of(i) : Stream.empty())
            .limit(count)
            .count();
        LOGGER.trace("Removed {} request(s), new total is {}.", () -> removed, this::count);
    }

    @Override
    public void keepOnly(final Duration interval) {
        LOGGER.trace("Resetting to within the last interval: {}.", interval);
        var threshold = DateUtil.zonedNow()
            .minus(interval);
        // Copy the tail set into a new collection, preventing leaking of the stale data.
        requests = new ConcurrentSkipListSet<>(requests.tailSet(threshold));
    }
}
