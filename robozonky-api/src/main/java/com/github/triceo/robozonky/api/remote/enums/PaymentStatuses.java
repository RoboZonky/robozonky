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

package com.github.triceo.robozonky.api.remote.enums;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class PaymentStatuses {

    private static final Pattern COMMA_SEPARATED = Pattern.compile("\\Q,\\E");

    public static PaymentStatuses valueOf(final String statuses) {
        // trim the surrounding []
        final String trimmed = statuses.trim();
        if (!trimmed.startsWith("[") || !trimmed.endsWith("]")) {
            throw new IllegalArgumentException("Expecting string in the format of [A, B, C], got " + statuses);
        }
        if (trimmed.length() == 2) { // only contains []
            return PaymentStatuses.of();
        }
        final String[] parts = PaymentStatuses.COMMA_SEPARATED.split(trimmed.substring(1, trimmed.length() - 1));
        if (parts.length == 1 && parts[0].trim().length() == 0) { // only contains whitespace
            return PaymentStatuses.of();
        }
        // and finally convert
        return PaymentStatuses.of(Stream.of(parts)
                          .map(String::trim)
                                          .map(PaymentStatus::valueOf)
                          .collect(Collectors.toList()));
    }

    public static PaymentStatuses of(final PaymentStatus... statuses) {
        return PaymentStatuses.of(Arrays.asList(statuses));
    }

    public static PaymentStatuses of(final Collection<PaymentStatus> statuses) {
        return new PaymentStatuses(statuses);
    }

    public static PaymentStatuses all() {
        return PaymentStatuses.of(PaymentStatus.values());
    }

    private final Set<PaymentStatus> statuses;

    private PaymentStatuses(final Collection<PaymentStatus> statuses) {
        this.statuses = statuses.isEmpty() ? Collections.emptySet() : EnumSet.copyOf(statuses);
    }

    public Set<PaymentStatus> getPaymentStatuses() {
        return Collections.unmodifiableSet(statuses);
    }

    @Override
    public String toString() {
        return statuses.stream().map(PaymentStatus::name).collect(Collectors.joining(", ", "[", "]"));
    }
}
