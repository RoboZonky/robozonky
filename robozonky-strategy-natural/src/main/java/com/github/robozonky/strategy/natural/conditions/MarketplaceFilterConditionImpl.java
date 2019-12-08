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

package com.github.robozonky.strategy.natural.conditions;

/**
 * Note: this class has a natural ordering that is inconsistent with equals.
 */
abstract class MarketplaceFilterConditionImpl implements MarketplaceFilterCondition,
                                                         Comparable<MarketplaceFilterConditionImpl> {

    private final boolean mayRequireRemoteRequests;

    protected MarketplaceFilterConditionImpl(final boolean mayRequireRemoteRequests) {
        this.mayRequireRemoteRequests = mayRequireRemoteRequests;
    }

    protected MarketplaceFilterConditionImpl() {
        this(false);
    }

    @Override
    public final boolean mayRequireRemoteRequests() {
        return mayRequireRemoteRequests;
    }

    @Override
    public String toString() {
        final String description = getDescription().orElse("N/A.");
        return this.getClass().getSimpleName() + " (" + description + ")";
    }

    @Override
    public int compareTo(final MarketplaceFilterConditionImpl o) {
        return Boolean.compare(mayRequireRemoteRequests(), o.mayRequireRemoteRequests());
    }

}
