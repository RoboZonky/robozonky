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

package com.github.robozonky.app.daemon.operations;

import java.util.Collection;

import com.github.robozonky.api.remote.entities.sanitized.Investment;
import com.github.robozonky.api.strategies.ParticipationDescriptor;
import com.github.robozonky.api.strategies.PurchaseStrategy;
import com.github.robozonky.app.daemon.Portfolio;
import com.github.robozonky.common.Tenant;

public class Purchasing extends StrategyExecutor<ParticipationDescriptor, PurchaseStrategy> {

    public Purchasing(final Tenant auth) {
        super(auth, auth::getPurchaseStrategy);
    }

    @Override
    protected boolean isBalanceUnderMinimum(final int current) { // there is no minimum in purchasing
        return false;
    }

    @Override
    protected long identify(final ParticipationDescriptor descriptor) {
        return descriptor.item().getId();
    }

    @Override
    protected Collection<Investment> execute(final Portfolio portfolio, final PurchaseStrategy strategy,
                                             final Collection<ParticipationDescriptor> marketplace) {
        return PurchasingSession.purchase(portfolio, getTenant(), marketplace, strategy);
    }
}
