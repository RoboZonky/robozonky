/*
 * Copyright 2016 Lukáš Petrovický
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

package com.github.triceo.robozonky.api.notifications;

import java.util.Collection;
import java.util.Collections;

import com.github.triceo.robozonky.api.remote.entities.Investment;

/**
 * Fired immediately after the strategy has finished evaluating loans and making investments.
 */
public final class ExecutionCompletedEvent implements Event {

    private final Collection<Investment> investment;
    private final int newBalance;

    public ExecutionCompletedEvent(final Collection<Investment> investment, final int newBalance) {
        this.investment = Collections.unmodifiableCollection(investment);
        this.newBalance = newBalance;
    }

    /**
     * @return The investments that were made.
     */
    public Collection<Investment> getInvestments() {
        return this.investment;
    }

    /**
     *
     * @return Account balance at the end of the investing algorithm.
     */
    public int getNewBalance() {
        return this.newBalance;
    }

}
