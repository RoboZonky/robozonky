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

import static java.lang.Math.max;
import static java.lang.Math.min;
import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Spliterator;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;
import java.util.function.LongConsumer;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

class PagingStreamsTest {

    private static List<String> strings(int start, int end) {
        return IntStream.range(start, end)
            .boxed()
            .map(Objects::toString)
            .collect(toList());
    }

    private static <T> Stream<T> parallel(long pageSize, List<T> items) {
        return PagingStreams.build(getSource(items), pageSize)
            .parallel();
    }

    private static <T> Stream<T> synchronous(long pageSize, List<T> items) {
        return PagingStreams.build(getSource(items), pageSize);
    }

    private static <T> Stream<T> dwindlingParallel(long pageSize, List<T> items) {
        return PagingStreams.build(getDwindlingSource(items), pageSize)
            .parallel();
    }

    private static <T> Stream<T> dwindlingSynchronous(long pageSize, List<T> items) {
        return PagingStreams.build(getDwindlingSource(items), pageSize);
    }

    private static <T> Stream<T> growingParallel(long pageSize, List<T> items) {
        return PagingStreams.build(getGrowingSource(items), pageSize)
            .parallel();
    }

    private static <T> Stream<T> growingSynchronous(long pageSize, List<T> items) {
        return PagingStreams.build(getGrowingSource(items), pageSize);
    }

    private static <T> PageSource<T> getSource(List<T> items) {
        return (offset, limit, totalSizeSink) -> {
            sleep();
            totalSizeSink.accept(items.size());
            return items.subList((int) offset, min((int) (offset + limit), items.size()));
        };
    }

    private static <T> PageSource<T> getDwindlingSource(List<T> items) {
        final AtomicInteger total = new AtomicInteger(items.size());
        return new PageSource<T>() {
            @Override
            public List<T> fetch(long offset, long limit, LongConsumer totalSizeSink) {
                sleep();
                synchronized (this) {
                    totalSizeSink.accept(total.getAndDecrement());
                    return items.subList((int) offset, (int) max(offset, min((int) (offset + limit), total.get())));
                }
            }
        };
    }

    private static <T> PageSource<T> getGrowingSource(List<T> items) {
        final AtomicInteger incrementer = new AtomicInteger(items.size());
        final IntUnaryOperator op = i -> i + (incrementer.getAndDecrement() > 0 ? incrementer.get() : 0);
        final AtomicInteger total = new AtomicInteger(items.size());
        return new PageSource<T>() {
            @Override
            public List<T> fetch(long offset, long limit, LongConsumer totalSizeSink) {
                sleep();
                synchronized (this) {
                    long previousTotal = total.get();
                    totalSizeSink.accept(previousTotal);
                    items.addAll(new ArrayList<>(items.subList(0, total.updateAndGet(op) - (int) previousTotal)));
                    return new ArrayList<>(items.subList((int) offset, (int) Math.min(offset + limit, items.size())));
                }
            }
        };
    }

    private static void sleep() {
        try {
            Thread.sleep(100);
        } catch (InterruptedException ignored) {
        }
    }

    @Test
    public void variousProperties() {
        Spliterator<String> spliterator = synchronous(4, strings(0, 9)).spliterator();
        assertThat(spliterator)
            .hasCharacteristics(PagingStreams.CHARACTERISTICS);
        // Consume the spliterator.
        assertThat(spliterator.estimateSize()).isEqualTo(9);
        spliterator.forEachRemaining(x -> {
            // NOOP
        });
        assertThat(spliterator.estimateSize()).isEqualTo(0);
    }

    @Test
    public void aSynchronousPagedSourceProcessesAllResultsUnlessPageSizeIsZero() {
        assertSoftly(softly -> {
            softly.assertThat(synchronous(4, strings(0, 9)))
                .hasSize(9);
            softly.assertThat(synchronous(4, strings(0, 4)))
                .hasSize(4);
            softly.assertThat(synchronous(0, strings(0, 4)))
                .hasSize(0);
        });
    }

    @Test
    public void aParallelPagedSourceProcessesAllResultsUnlessPageSizeIsZero() {
        assertSoftly(softly -> {
            softly.assertThat(parallel(4, strings(0, 9)))
                .hasSize(9);
            softly.assertThat(parallel(4, strings(0, 4)))
                .hasSize(4);
            softly.assertThat(parallel(0, strings(0, 4)))
                .hasSize(0);
        });
    }

    @Test
    public void aSynchronousPagedSourceMaintainsOrderWhenCollectingToOrderedStructures() {
        assertThat(synchronous(2, strings(0, 9)).collect(toList()))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8");
    }

    @Test
    public void aParallelPagedSourceMaintainsOrderWhenCollectingToOrderedStructures() {
        assertThat(parallel(2, strings(0, 9)).collect(toList()))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8");
    }

    @Test
    public void aSynchronousForEachTraversalOfAPagedSourceMaintainsOrder() {
        assertThat(synchronous(2, strings(0, 9)))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8");
    }

    @Test
    public void aParallelForEachTraversalOfAPagedSourceDoesNotMaintainOrder() {
        List<String> each = new CopyOnWriteArrayList<>();
        parallel(2, strings(0, 9)).forEach(each::add);
        assertThat(each)
            .containsExactlyInAnyOrder("0", "1", "2", "3", "4", "5", "6", "7", "8");
    }

    @Test
    public void aParallelForEachOrderedTraversalOfAPagedSourceDoesMaintainOrder() {
        List<String> each = new CopyOnWriteArrayList<>();
        parallel(2, strings(0, 9)).forEachOrdered(each::add);
        assertThat(each)
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8");
    }

    @Test
    public void synchronousSourceHasDwindlingResults() {
        assertThat(dwindlingSynchronous(2, strings(0, 9)))
            .containsExactly("0", "1", "2", "3", "4", "5");
    }

    @Test
    public void parallelSourceWithDwindlingResultsCompletes() {
        assertThat(dwindlingParallel(2, strings(0, 9)))
            .containsExactly("0", "1", "2", "3", "4", "5");
    }

    @Test
    public void synchronousSourceWithGrowingResultsStopsAtCap() {
        assertThat(growingSynchronous(2, strings(0, 9)))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8");
    }

    @Test
    public void parallelSourceWithGrowingSourceStopsAtCap() {
        assertThat(growingParallel(2, strings(0, 9)))
            .containsExactly("0", "1", "2", "3", "4", "5", "6", "7", "8");
    }

}
