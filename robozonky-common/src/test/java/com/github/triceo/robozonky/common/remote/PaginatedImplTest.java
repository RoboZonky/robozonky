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

import java.util.Collections;

import com.github.triceo.robozonky.api.remote.LoanApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class PaginatedImplTest {

    @Test
    public void constructor() {
        final PaginatedApi<Loan, LoanApi> api = Mockito.mock(PaginatedApi.class);
        final int pageSize = 20;
        // when execute is called with the right parameters, we pretend the API returned no results
        Mockito.when(api.execute(ArgumentMatchers.any(), ArgumentMatchers.eq(0), ArgumentMatchers.eq(pageSize)))
                .thenReturn(new PaginatedResult<>(Collections.emptyList(), 0, 0));
        final Paginated<Loan> p = new PaginatedImpl<>(api);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(p.getExpectedPageSize()).isEqualTo(pageSize);
            softly.assertThat(p.getPageId()).isEqualTo(0);
            softly.assertThat(p.getTotalItemCount()).isEqualTo(0);
            softly.assertThat(p.getItemsOnPage()).isEmpty();
            softly.assertThat(p.nextPage()).isFalse();
        });
    }

    @Test
    public void nextPageMissing() {
        final PaginatedApi<Loan, LoanApi> api = Mockito.mock(PaginatedApi.class);
        final int pageSize = 1;
        // when execute calls for first page, we pretend the API returned 1 result
        Mockito.when(api.execute(ArgumentMatchers.any(), ArgumentMatchers.eq(0), ArgumentMatchers.eq(pageSize)))
                .thenReturn(new PaginatedResult<>(Collections.singleton(new Loan(1, 200)), 0, 1));
        // when execute calls for second page, we pretend the API returned no results
        Mockito.when(api.execute(ArgumentMatchers.any(), ArgumentMatchers.eq(1), ArgumentMatchers.eq(pageSize)))
                .thenReturn(new PaginatedResult<>(Collections.emptyList(), 1, 1));
        final Paginated<Loan> p = new PaginatedImpl<>(api, pageSize);
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(p.getExpectedPageSize()).isEqualTo(pageSize);
            softly.assertThat(p.getPageId()).isEqualTo(0);
            softly.assertThat(p.getTotalItemCount()).isEqualTo(1);
            softly.assertThat(p.getItemsOnPage()).hasSize(1);
        });
        Assertions.assertThat(p.nextPage()).isFalse();
        SoftAssertions.assertSoftly(softly -> {
            softly.assertThat(p.getExpectedPageSize()).isEqualTo(pageSize);
            softly.assertThat(p.getPageId()).isEqualTo(1);
            softly.assertThat(p.getTotalItemCount()).isEqualTo(1);
            softly.assertThat(p.getItemsOnPage()).hasSize(0);
        });
    }

}
