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

package com.github.triceo.robozonky.strategy.rules;

import java.util.Comparator;

import com.github.triceo.robozonky.strategy.rules.facts.AcceptedLoan;

public class AcceptedLoanComparator implements Comparator<AcceptedLoan> {

    @Override
    public int compare(final AcceptedLoan acceptedLoan, final AcceptedLoan t1) {
        // most important first
        final Comparator<AcceptedLoan> byPriority = Comparator.comparing(AcceptedLoan::getPriority).reversed();
        // then oldest
        final Comparator<AcceptedLoan> byId = Comparator.comparing(AcceptedLoan::getId);
        return byPriority.thenComparing(byId).compare(acceptedLoan, t1);
    }
}
