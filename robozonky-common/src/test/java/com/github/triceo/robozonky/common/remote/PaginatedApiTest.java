/*
 * Copyright 2017 Lukáš Petrovický
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

package com.github.triceo.robozonky.common.remote;

import java.util.Optional;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.LoanApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

@SuppressWarnings({"rawtypes", "unchecked"})
public class PaginatedApiTest {

    @Test
    public void redirectAndLiveQuery() {
        // FIXME will fail when Zonky API is down
        final RoboZonkyFilter f = new RoboZonkyFilter();
        final PaginatedApi<Loan, LoanApi> pa = new PaginatedApi<>(LoanApi.class, "http://api.zonky.cz", null);
        final PaginatedResult<Loan> loans = pa.execute(LoanApi::items, Sort.unspecified(), 0, 10, f);
        Assertions.assertThat(loans.getCurrentPageId()).isEqualTo(0);
        Assertions.assertThat(loans.getTotalResultCount()).isGreaterThanOrEqualTo(0);
    }

    @Test
    public void checkSort() {
        final PaginatedApi p = Mockito.spy(new PaginatedApi(null, null, null));
        Mockito.doReturn(null).when(p)
                .execute(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        final Function f = o -> null;
        p.execute(f);
        Mockito.verify(p)
                .execute(ArgumentMatchers.eq(f), ArgumentMatchers.notNull(), ArgumentMatchers.notNull());
    }

    @Test
    public void checkFilter() {
        final PaginatedApi p = Mockito.spy(new PaginatedApi(null, null, null));
        Mockito.doReturn(null).when(p).execute(ArgumentMatchers.any(), ArgumentMatchers.any());
        final Function f = o -> null;
        final Sort s = Mockito.mock(Sort.class);
        final RoboZonkyFilter filter = new RoboZonkyFilter();
        p.execute(f, s, filter);
        Mockito.verify(s).apply(filter);
        Mockito.verify(p).execute(ArgumentMatchers.eq(f), ArgumentMatchers.notNull());
    }

    @Test
    public void checkPagination() {
        final PaginatedApi p = Mockito.spy(new PaginatedApi(null, null, null));
        Mockito.doReturn(null)
                .when(p).execute(ArgumentMatchers.any(), ArgumentMatchers.any(), ArgumentMatchers.any());
        final Function f = o -> null;
        final Sort s = Mockito.mock(Sort.class);
        final int total = 1000;
        final RoboZonkyFilter filter = Mockito.mock(RoboZonkyFilter.class);
        Mockito.when(filter.getLastResponseHeader(ArgumentMatchers.eq("X-Total")))
                .thenReturn(Optional.of("" + total));
        final PaginatedResult result = p.execute(f, s, 1, 10, filter);
        Assertions.assertThat(result.getTotalResultCount()).isEqualTo(total);
        Mockito.verify(filter).setRequestHeader(ArgumentMatchers.eq("X-Size"), ArgumentMatchers.eq("10"));
        Mockito.verify(filter).setRequestHeader(ArgumentMatchers.eq("X-Page"), ArgumentMatchers.eq("1"));
        Mockito.verify(p).execute(ArgumentMatchers.eq(f), ArgumentMatchers.notNull(), ArgumentMatchers.eq(filter));
    }

}
