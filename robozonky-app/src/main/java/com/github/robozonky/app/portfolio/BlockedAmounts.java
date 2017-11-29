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

package com.github.robozonky.app.portfolio;

import java.util.ArrayList;
import java.util.Collection;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.common.remote.Zonky;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class BlockedAmounts implements PortfolioBased {

    public static final BlockedAmounts INSTANCE = new BlockedAmounts();

    private static final Logger LOGGER = LoggerFactory.getLogger(BlockedAmounts.class);

    private final AtomicReference<Collection<BlockedAmount>> blockedAmounts = new AtomicReference<>(new ArrayList<>(0));

    private BlockedAmounts() {
        // singleton
    }

    public void accept(final Portfolio portfolio, final Zonky zonky) {
        LOGGER.trace("Starting.");
        final Collection<BlockedAmount> presentBlockedAmounts = zonky.getBlockedAmounts().collect(Collectors.toList());
        final Collection<BlockedAmount> previousBlockedAmounts = blockedAmounts.getAndSet(presentBlockedAmounts);
        final SortedSet<BlockedAmount> difference = presentBlockedAmounts.stream()
                .filter(ba -> !previousBlockedAmounts.contains(ba))
                .collect(Collectors.collectingAndThen(Collectors.toSet(), TreeSet::new));
        portfolio.newBlockedAmounts(zonky, difference);
        LOGGER.trace("Finished.");
    }
}
