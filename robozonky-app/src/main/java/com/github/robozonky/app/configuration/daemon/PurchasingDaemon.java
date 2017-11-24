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

import java.time.Duration;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import com.github.robozonky.api.remote.entities.Participation;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.purchasing.Purchasing;

class PurchasingDaemon extends DaemonOperation {

    public PurchasingDaemon(final Authenticated auth, final Supplier<Optional<PurchaseStrategy>> strategy,
                            final Duration maximumSleepPeriod, final Duration refreshPeriod, final boolean isDryRun) {
        super(auth, api -> api.run(zonky -> {
            final Collection<Participation> p = zonky.getAvailableParticipations().collect(Collectors.toList());
            new Purchasing(strategy, zonky, maximumSleepPeriod, isDryRun).apply(p);
        }), refreshPeriod);
    }
}
