/*
 *
 *  * Copyright 2016 Lukáš Petrovický
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  *
 *  *      http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 * /
 */
package com.github.triceo.zonkybot.remote;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class InvestmentStatuses {

    public static InvestmentStatuses of(final InvestmentStatus... statuses) {
        return of(Arrays.asList(statuses));
    }

    public static InvestmentStatuses of(final List<InvestmentStatus> statuses) {
        return new InvestmentStatuses(statuses);
    }

    public static InvestmentStatuses all() {
        return of(InvestmentStatus.values());
    }

    private final Set<InvestmentStatus> statuses;

    private InvestmentStatuses(final Collection<InvestmentStatus> statuses) {
        this.statuses = EnumSet.copyOf(statuses);
    }

    public Set<InvestmentStatus> getInvestmentStatuses() {
        return Collections.unmodifiableSet(statuses);
    }

    @Override
    public String toString() {
        return statuses.stream().collect(Collectors.mapping(InvestmentStatus::name, Collectors.joining(", ", "[", "]")));
    }
}
