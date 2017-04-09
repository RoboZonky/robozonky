/*
 * Copyright 2017 Lukáš Petrovický
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

class ProcessedLoanComparator implements Comparator<ProcessedLoan> {

    @Override
    public int compare(final ProcessedLoan acceptedLoan, final ProcessedLoan t1) {
        // most important first
        final Comparator<ProcessedLoan> byPriority = Comparator.comparing(ProcessedLoan::getPriority).reversed();
        // then oldest
        final Comparator<ProcessedLoan> byRecent =
                (p1, p2) -> Comparator.comparing((ProcessedLoan p) -> p.getLoan().getDatePublished()).compare(p1, p2);
        return byPriority.thenComparing(byRecent).compare(acceptedLoan, t1);
    }
}
