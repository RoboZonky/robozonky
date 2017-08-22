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

package com.github.robozonky.common.remote;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

class SortImpl<S> implements Sort<S> {

    private final Map<Field<S>, Boolean> ordering = new LinkedHashMap<>(1);

    SortImpl(final Field<S> first, final boolean ascending) {
        this.ordering.put(first, ascending);
    }

    private synchronized String getOrdering() {
        return ordering.entrySet().stream()
                .map(e -> {
                    final String field = e.getKey().id();
                    return e.getValue() ? field : "-" + field;
                }).collect(Collectors.joining(","));
    }

    @Override
    public synchronized SortImpl<S> thenBy(final Field<S> field, final boolean ascending) {
        if (ordering.containsKey(field)) {
            throw new IllegalArgumentException("Field already used: " + field);
        }
        this.ordering.put(field, ascending);
        return this;
    }

    @Override
    public void apply(final RoboZonkyFilter filter) {
        final String ordering = this.getOrdering();
        if (ordering.length() > 0) {
            filter.setRequestHeader("X-Order", ordering);
        }
    }
}
