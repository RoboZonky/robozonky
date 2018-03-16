/*
 * Copyright 2018 The RoboZonky Project
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

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.BlockedAmount;
import com.github.robozonky.app.authentication.Authenticated;

public class BlockedAmounts implements PortfolioDependant {

    private final AtomicReference<Collection<BlockedAmount>> blockedAmounts =
            new AtomicReference<>(Collections.emptyList());

    public void accept(final Portfolio portfolio, final Authenticated auth) {
        final Collection<BlockedAmount> presentBlockedAmounts =
                auth.call(zonky -> zonky.getBlockedAmounts().collect(Collectors.toList()));
        final Collection<BlockedAmount> previousBlockedAmounts = blockedAmounts.getAndSet(presentBlockedAmounts);
        final Set<BlockedAmount> difference = presentBlockedAmounts.stream()
                .filter(ba -> !previousBlockedAmounts.contains(ba))
                .collect(Collectors.toSet());
        difference.forEach(ba -> portfolio.newBlockedAmount(auth, ba));
    }
}
