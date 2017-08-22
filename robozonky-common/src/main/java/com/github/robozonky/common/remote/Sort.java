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

public interface Sort<S> {

    static <S> Sort<S> unspecified() {
        return new Sort<S>() {

            @Override
            public Sort<S> thenBy(final Field<S> field, final boolean ascending) {
                throw new IllegalStateException("Cannot sort with unspecified.");
            }

            @Override
            public void apply(final RoboZonkyFilter filter) {
                // do not apply any ordering
            }
        };
    }

    static <S> Sort<S> by(final Field<S> field) {
        return Sort.by(field, true);
    }

    static <S> Sort<S> by(final Field<S> field, final boolean ascending) {
        return new SortImpl<>(field, ascending);
    }

    default Sort<S> thenBy(final Field<S> field) {
        return this.thenBy(field, true);
    }

    Sort<S> thenBy(Field<S> field, boolean ascending);

    void apply(RoboZonkyFilter filter);
}
