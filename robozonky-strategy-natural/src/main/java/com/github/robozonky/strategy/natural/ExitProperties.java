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

package com.github.robozonky.strategy.natural;

import java.time.LocalDate;

public class ExitProperties {

    private final LocalDate accountTermination, selloffStart;

    public ExitProperties(final LocalDate accountTermination) {
        this(accountTermination, accountTermination.minusMonths(3));
    }

    public ExitProperties(final LocalDate accountTermination, final LocalDate selloffStart) {
        this.accountTermination = accountTermination;
        if (!selloffStart.isBefore(accountTermination)) {
            throw new IllegalArgumentException("Sell-off must start before the account termination date.");
        }
        this.selloffStart = selloffStart;
    }

    public LocalDate getAccountTermination() {
        return accountTermination;
    }

    public LocalDate getSelloffStart() {
        return selloffStart;
    }

    @Override
    public String toString() {
        return "ExitProperties{" +
                "accountTermination=" + accountTermination +
                ", selloffStart=" + selloffStart +
                '}';
    }
}
