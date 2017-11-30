/*
 * Copyright 2017 The RoboZonky Project
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

package com.github.robozonky.app.configuration.daemon;

import java.util.Collection;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.portfolio.Portfolio;
import com.github.robozonky.app.portfolio.PortfolioDependant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BlockedAmounts implements PortfolioDependant {

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockedAmounts.class);

    private final AtomicReference<Collection<BlockedAmount>> blockedAmounts =
            new AtomicReference<>(Collections.emptyList());

    BlockedAmounts() {
    }

    public void accept(final Portfolio portfolio, final Authenticated auth) {
        LOGGER.trace("Starting.");
        final Collection<BlockedAmount> presentBlockedAmounts =
                auth.call(zonky -> zonky.getBlockedAmounts().collect(Collectors.toList()));
        final Collection<BlockedAmount> previousBlockedAmounts = blockedAmounts.getAndSet(presentBlockedAmounts);
        final SortedSet<BlockedAmount> difference = presentBlockedAmounts.stream()
                .filter(ba -> !previousBlockedAmounts.contains(ba))
                .collect(Collectors.collectingAndThen(Collectors.toSet(), TreeSet::new));
        auth.run(zonky -> portfolio.newBlockedAmounts(zonky, difference));
        LOGGER.trace("Finished.");
    }
}
