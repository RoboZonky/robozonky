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

import static java.lang.Math.min;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Consumer;
import java.util.stream.StreamSupport;

final class PagingSpliterator<T> implements Spliterator<T> {

    private static final int SIZED_OR_SUBSIZED = SIZED
            | SUBSIZED;
    private static final int IMUTABLE_OR_ORDERED = IMMUTABLE
            | ORDERED;
    private final PageSource<T> supplier;
    private final long pageSize;
    private long start;
    private long end;
    private Spliterator<T> currentPage;
    private Spliterator<T> danglingFirstPage;

    PagingSpliterator(final PageSource<T> supplier, final long start, final long end, final long pageSize) {
        this.supplier = supplier;
        this.start = start;
        this.end = end;
        this.pageSize = pageSize;
    }

    /**
     * Ensure that the sub-spliterator provided by the List is compatible with
     * ours, i.e. is {@code SIZED_OR_SUBSIZED}. For standard List implementations,
     * the spliterators are, so the costs of dumping into an intermediate array
     * in the other case is irrelevant.
     */
    private static <X> Spliterator<X> spliterator(final List<X> list) {
        Spliterator<X> original = list.spliterator();
        if ((original.characteristics()
                & SIZED_OR_SUBSIZED) == SIZED_OR_SUBSIZED) {
            return original;
        }
        var array = StreamSupport.stream(original, false)
            .toArray();
        return Spliterators.spliterator(array, IMUTABLE_OR_ORDERED);
    }

    public static <X> Spliterator<X> build(PageSource<X> source, long pageSize) {
        if (pageSize == 0) {
            return Spliterators.emptySpliterator();
        }
        final PagingSpliterator<X> pgSp = new PagingSpliterator<>(source, 0, 0, pageSize);
        pgSp.danglingFirstPage = spliterator(source.fetch(0, pageSize, l -> pgSp.end = l));
        return pgSp;
    }

    @Override
    public final boolean tryAdvance(final Consumer<? super T> action) {
        do {
            if (ensurePage().tryAdvance(action)) {
                return true;
            }
            if (start >= end) {
                return false;
            }
            currentPage = null;
        } while (true);
    }

    @Override
    public final void forEachRemaining(final Consumer<? super T> action) {
        do {
            ensurePage().forEachRemaining(action);
            currentPage = null;
        } while (start < end);
    }

    @Override
    public final Spliterator<T> trySplit() {
        if (danglingFirstPage != null) {
            final Spliterator<T> fp = danglingFirstPage;
            danglingFirstPage = null;
            start = fp.getExactSizeIfKnown();
            return fp;
        } else if (currentPage != null) {
            return currentPage.trySplit();
        } else if (end - start > pageSize) {
            long mid = (start + end) >>> 1;
            mid = mid / pageSize * pageSize;
            if (mid == start) {
                mid += pageSize;
            }
            return new PagingSpliterator<>(supplier, start, start = mid, pageSize);
        }
        return ensurePage().trySplit();
    }

    @Override
    public final long estimateSize() {
        if (currentPage == null) {
            return end - start;
        }
        return currentPage.estimateSize();
    }

    @Override
    public final int characteristics() {
        return PagingStreams.CHARACTERISTICS;
    }

    /**
     * Fetch data immediately before traversing or sub-page splitting.
     */
    private Spliterator<T> ensurePage() {
        if (danglingFirstPage != null) {
            final Spliterator<T> fp = danglingFirstPage;
            danglingFirstPage = null;
            currentPage = fp;
            start = fp.getExactSizeIfKnown();
            return fp;
        } else if (currentPage != null) {
            return currentPage;
        } else if (start >= end) {
            return Spliterators.emptySpliterator();
        }
        var spliterator = spliterator(supplier.fetch(start, min(end - start, pageSize), l -> end = min(l, end)));
        start += spliterator.getExactSizeIfKnown();
        currentPage = spliterator;
        return spliterator;
    }

}
