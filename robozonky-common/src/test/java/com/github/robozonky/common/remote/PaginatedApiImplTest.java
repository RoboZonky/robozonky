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

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import org.jboss.resteasy.client.jaxrs.ResteasyClient;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.isNotNull;
import static org.mockito.Mockito.*;

class PaginatedApiImplTest<S, T> {

    @Test
    void checkFilter() {
        final Function<T, S> f = o -> null;
        final Select sel = mock(Select.class);
        final RoboZonkyFilter filter = new RoboZonkyFilter();
        final PaginatedApi<S, T> p = spy(new PaginatedApi<>(null, null, null, null));
        doReturn(null).when(p).execute(eq(f), eq(filter));
        p.execute(f, sel, filter);
        verify(sel).accept(filter);
        verify(p).execute(eq(f), any(RoboZonkyFilter.class));
    }

    @Test
    void checkSimple() {
        final Function<T, S> f = o -> null;
        final PaginatedApi<S, T> p = spy(new PaginatedApi<>(null, null, null, null));
        doReturn(null).when(p).execute(eq(f), any());
        p.execute(f);
        verify(p).execute(eq(f), isNotNull());
    }

    @Test
    void checkPagination() {
        final PaginatedApi<S, T> p = spy(new PaginatedApi<>(null, null, null, mock(ResteasyClient.class)));
        doReturn(null).when(p).execute(any(), any(), any());
        final Function<T, List<S>> f = o -> Collections.emptyList();
        final Select sel = mock(Select.class);
        final int total = 1000;
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        when(filter.getLastResponseHeader(eq("X-Total")))
                .thenReturn(Optional.of("" + total));
        final PaginatedResult<S> result = p.execute(f, sel, 1, 10, filter);
        assertThat(result.getTotalResultCount()).isEqualTo(total);
        verify(filter).setRequestHeader(eq("X-Size"), eq("10"));
        verify(filter).setRequestHeader(eq("X-Page"), eq("1"));
        verify(p).execute(eq(f), eq(sel), eq(filter));
    }
}
