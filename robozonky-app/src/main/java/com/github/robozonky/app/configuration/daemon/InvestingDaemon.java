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

import java.time.temporal.TemporalAmount;
import java.util.Collection;
import java.util.stream.Collectors;

import com.github.robozonky.api.Refreshable;
import com.github.robozonky.api.marketplaces.Marketplace;
import com.github.robozonky.api.strategies.InvestmentStrategy;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.app.authentication.Authenticated;
import com.github.robozonky.app.investing.Investing;
import com.github.robozonky.app.investing.Investor;

class InvestingDaemon extends DaemonOperation {

    public InvestingDaemon(final Authenticated auth, final Investor.Builder builder, final Marketplace marketplace,
                           final Refreshable<InvestmentStrategy> strategy, final TemporalAmount maximumSleepPeriod) {
        super(auth, zonky -> {
            marketplace.registerListener((loans) -> {
                if (loans == null) {
                    return;
                }
                final Collection<LoanDescriptor> descriptors = loans.stream()
                        .map(LoanDescriptor::new)
                        .collect(Collectors.toList());
                new Investing(builder, strategy, zonky, maximumSleepPeriod).apply(descriptors);
            });
            marketplace.run();
        });
    }
}
