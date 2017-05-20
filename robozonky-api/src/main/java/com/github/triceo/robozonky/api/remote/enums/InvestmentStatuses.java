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

package com.github.triceo.robozonky.api.remote.enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class InvestmentStatuses {

    private static final Pattern COMMA_SEPARATED = Pattern.compile("\\Q,\\E");

    public static InvestmentStatuses valueOf(final String statuses) {
        // trim the surrounding []
        final String trimmed = statuses.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Expecting string in the format of [A, B, C], got " + statuses);
        }
        if (trimmed.length() == 2) { // only contains []
            return InvestmentStatuses.of();
        }
        final String[] parts = InvestmentStatuses.COMMA_SEPARATED.split(trimmed.substring(1, trimmed.length() - 1));
        if (parts.length == 1 && parts[0].trim().length() == 0) { // only contains whitespace
            return InvestmentStatuses.of();
        }
        // and finally convert
        return of(Stream.of(parts)
                .map(String::trim)
                .map(InvestmentStatus::valueOf)
                .collect(Collectors.toList()));
    }

    public static InvestmentStatuses of(final InvestmentStatus... statuses) {
        return InvestmentStatuses.of(Arrays.asList(statuses));
    }

    public static InvestmentStatuses of(final Collection<InvestmentStatus> statuses) {
        return new InvestmentStatuses(statuses);
    }

    public static InvestmentStatuses all() {
        return InvestmentStatuses.of(InvestmentStatus.values());
    }

    private final Set<InvestmentStatus> statuses;

    private InvestmentStatuses(final Collection<InvestmentStatus> statuses) {
        this.statuses = statuses.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(statuses);
    }

    public Set<InvestmentStatus> getInvestmentStatuses() {
        return Collections.unmodifiableSet(statuses);
    }

    @Override
    public String toString() {
        return statuses.stream().map(InvestmentStatus::name).collect(Collectors.joining(", ", "[", "]"));
    }
}
