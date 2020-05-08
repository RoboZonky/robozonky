/*
 * Copyright 2020 The RoboZonky Project
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

package com.github.robozonky.internal.util.stream;

import static java.util.Spliterator.IMMUTABLE;
import static java.util.Spliterator.ORDERED;
import static java.util.Spliterator.SIZED;
import static java.util.Spliterator.SUBSIZED;

import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import com.github.robozonky.internal.Settings;

public final class PagingStreams {

    static final int CHARACTERISTICS = IMMUTABLE
            | ORDERED
            | SIZED
            | SUBSIZED;

    public static <T> Stream<T> build(final PageSource<T> source) {
        return build(source, Settings.INSTANCE.getDefaultApiPageSize());
    }

    public static <T> Stream<T> build(final PageSource<T> source, final long pageSize) {
        return StreamSupport.stream(() -> PagingSpliterator.build(source, pageSize), CHARACTERISTICS, false);
    }

}
