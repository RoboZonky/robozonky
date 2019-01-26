/*
 * Copyright 2019 The RoboZonky Project
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

import java.util.List;
import java.util.function.Function;
import java.util.function.LongConsumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.github.rutledgepaulv.pagingstreams.PageSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntityCollectionPageSourceTest {

    private static final Select SELECT = new Select();
    private static final Function<Object, List<Integer>> FUNCTION = o -> IntStream.range(0, 1000)
            .boxed()
            .collect(Collectors.toList());
    private static final int PAGE_SIZE = 100;

    @Mock
    private PaginatedApi<Integer, Object> api;

    @Test
    void firstPage() {
        final List<Integer> allResults = FUNCTION.apply(null);
        final List<Integer> subpage = allResults.subList(0, PAGE_SIZE);
        when(api.execute(eq(FUNCTION), eq(SELECT), eq(0), eq(PAGE_SIZE)))
                .thenReturn(new PaginatedResult<>(subpage, allResults.size()));
        final PageSource<Integer> source = new EntityCollectionPageSource<>(api, FUNCTION, SELECT, PAGE_SIZE);
        final LongConsumer consumer = mock(LongConsumer.class);
        final List<Integer> result = source.fetch(0, 1, consumer);
        assertThat(result).containsExactly(subpage.toArray(new Integer[PAGE_SIZE]));
        verify(consumer).accept(eq((long) allResults.size()));
    }

    @Test
    void negativePageWorksAsFirstPage() {
        final List<Integer> allResults = FUNCTION.apply(null);
        final List<Integer> subpage = allResults.subList(0, PAGE_SIZE);
        when(api.execute(eq(FUNCTION), eq(SELECT), eq(0), eq(PAGE_SIZE)))
                .thenReturn(new PaginatedResult<>(subpage, allResults.size()));
        final PageSource<Integer> source = new EntityCollectionPageSource<>(api, FUNCTION, SELECT, PAGE_SIZE);
        final LongConsumer consumer = mock(LongConsumer.class);
        final List<Integer> result = source.fetch(-1, 1, consumer);
        assertThat(result).containsExactly(subpage.toArray(new Integer[PAGE_SIZE]));
        verify(consumer).accept(eq((long) allResults.size()));
    }

    @Test
    void secondPage() {
        final List<Integer> allResults = FUNCTION.apply(null);
        final List<Integer> subpage = allResults.subList(PAGE_SIZE, 2 * PAGE_SIZE);
        when(api.execute(eq(FUNCTION), eq(SELECT), eq(1), eq(PAGE_SIZE)))
                .thenReturn(new PaginatedResult<>(subpage, allResults.size()));
        final PageSource<Integer> source = new EntityCollectionPageSource<>(api, FUNCTION, SELECT, PAGE_SIZE);
        final LongConsumer consumer = mock(LongConsumer.class);
        final List<Integer> result = source.fetch(PAGE_SIZE, 1, consumer);
        assertThat(result).containsExactly(subpage.toArray(new Integer[PAGE_SIZE]));
        verify(consumer).accept(eq((long) allResults.size()));
    }
}
