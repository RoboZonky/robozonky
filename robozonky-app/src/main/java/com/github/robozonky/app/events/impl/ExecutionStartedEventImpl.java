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

package com.github.robozonky.app.events.impl;

import java.util.Collection;
import java.util.Collections;
import java.util.StringJoiner;

import com.github.robozonky.api.notifications.ExecutionStartedEvent;
import com.github.robozonky.api.strategies.LoanDescriptor;
import com.github.robozonky.api.strategies.PortfolioOverview;

final class ExecutionStartedEventImpl extends AbstractEventImpl implements ExecutionStartedEvent {

    private final Collection<LoanDescriptor> loanDescriptors;
    private final PortfolioOverview portfolioOverview;

    public ExecutionStartedEventImpl(final Collection<LoanDescriptor> loanDescriptors, final PortfolioOverview portfolio) {
        this.loanDescriptors = Collections.unmodifiableCollection(loanDescriptors);
        this.portfolioOverview = portfolio;
    }

    @Override
    public PortfolioOverview getPortfolioOverview() {
        return portfolioOverview;
    }

    @Override
    public Collection<LoanDescriptor> getLoanDescriptors() {
        return this.loanDescriptors;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", ExecutionStartedEventImpl.class.getSimpleName() + "[", "]")
                .add("super=" + super.toString())
                .add("portfolioOverview=" + portfolioOverview)
                .toString();
    }
}
