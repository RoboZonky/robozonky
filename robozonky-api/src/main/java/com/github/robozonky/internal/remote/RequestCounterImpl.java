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

package com.github.robozonky.internal.remote;

import java.time.Duration;
import java.time.Instant;
import java.util.SortedMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import com.github.robozonky.internal.test.DateUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

final class RequestCounterImpl implements RequestCounter {

    private static final Logger LOGGER = LogManager.getLogger(RequestCounterImpl.class);

    private final AtomicLong counter = new AtomicLong();
    private final AtomicReference<SortedMap<Instant, Long>> requests = new AtomicReference<>(new ConcurrentSkipListMap<>());

    @Override
    public long mark() {
        final long id = counter.getAndIncrement();
        requests.get().put(DateUtil.now(), id);
        return id;
    }

    @Override
    public long current() {
        final SortedMap<Instant, Long> actual = requests.get();
        return actual.get(actual.lastKey());
    }

    @Override
    public int count() {
        return requests.get().size();
    }

    @Override
    public int count(final Duration interval) {
        final Instant threshold = DateUtil.now().minus(interval);
        return requests.get().tailMap(threshold).size();
    }

    @Override
    public void cut(final int count) {
        final SortedMap<Instant, Long> actualRequests = requests.get();
        actualRequests.keySet().stream()
                .limit(count)
                .forEach(actualRequests::remove);
        LOGGER.trace("Removed {} request(s), new total is {}.", count, count());
    }

    @Override
    public void keepOnly(final Duration interval) {
        LOGGER.trace("Resetting within the last interval: {}.", interval);
        final Instant threshold = DateUtil.now().minus(interval);
        requests.set(requests.get().tailMap(threshold));
    }
}
