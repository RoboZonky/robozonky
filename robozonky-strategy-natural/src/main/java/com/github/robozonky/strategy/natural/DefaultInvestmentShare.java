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

package com.github.robozonky.strategy.natural;

class DefaultInvestmentShare {

    private final int maximumShareInPercent;

    public DefaultInvestmentShare() {
        this(100);
    }

    public DefaultInvestmentShare(final int maximumShareInPercent) {
        if (maximumShareInPercent < getMinimumShareInPercent()) {
            throw new IllegalArgumentException("Maximum share must be at least 0.");
        } else if (maximumShareInPercent > 100) {
            throw new IllegalArgumentException("Maximum share must be 100 at most.");
        }
        this.maximumShareInPercent = maximumShareInPercent;
    }

    public int getMinimumShareInPercent() {
        return 0;
    }

    public int getMaximumShareInPercent() {
        return maximumShareInPercent;
    }

    @Override
    public String toString() {
        return "DefaultInvestmentShare{" +
                "minimumShareInPercent=" + getMinimumShareInPercent() +
                ", maximumShareInPercent=" + maximumShareInPercent +
                '}';
    }
}
