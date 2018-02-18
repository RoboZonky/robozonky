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

import java.util.Collections;

import com.github.robozonky.api.remote.LoanApi;
import com.github.robozonky.api.remote.entities.RawLoan;
import com.github.robozonky.internal.api.Settings;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class PaginatedImplTest {

    @Test
    void constructor() {
        final PaginatedApi<RawLoan, LoanApi> api = mock(PaginatedApi.class);
        final int pageSize = Settings.INSTANCE.getDefaultApiPageSize();
        // when execute is called with the right parameters, we pretend the API returned no results
        when(api.execute(any(), any(), any(), eq(0), eq(pageSize)))
                .thenReturn(new PaginatedResult<>(Collections.emptyList(), 0, 0));
        final Paginated<RawLoan> p = new PaginatedImpl<>(api);
        assertSoftly(softly -> {
            softly.assertThat(p.getExpectedPageSize()).isEqualTo(pageSize);
            softly.assertThat(p.getPageId()).isEqualTo(0);
            softly.assertThat(p.getTotalItemCount()).isEqualTo(0);
            softly.assertThat(p.getItemsOnPage()).isEmpty();
            softly.assertThat(p.nextPage()).isFalse();
        });
    }

    @Test
    void nextPageMissing() {
        final RawLoan loan = mock(RawLoan.class);
        when(loan.getId()).thenReturn(1);
        when(loan.getAmount()).thenReturn(200.0);
        when(loan.getRemainingInvestment()).thenReturn(200.0);
        final PaginatedApi<RawLoan, LoanApi> api = mock(PaginatedApi.class);
        final int pageSize = 1;
        // when execute calls for first page, we pretend the API returned 1 result
        when(api.execute(any(), any(), any(), eq(0), eq(pageSize)))
                .thenReturn(new PaginatedResult<>(Collections.singleton(loan), 0, 1));
        // when execute calls for second page, we pretend the API returned no results
        when(api.execute(any(), any(), any(), eq(1), eq(pageSize)))
                .thenReturn(new PaginatedResult<>(Collections.emptyList(), 1, 1));
        final Paginated<RawLoan> p = new PaginatedImpl<>(api, new Select(), Sort.unspecified(), pageSize);
        assertSoftly(softly -> {
            softly.assertThat(p.getExpectedPageSize()).isEqualTo(pageSize);
            softly.assertThat(p.getPageId()).isEqualTo(0);
            softly.assertThat(p.getTotalItemCount()).isEqualTo(1);
            softly.assertThat(p.getItemsOnPage()).hasSize(1);
        });
        assertThat(p.nextPage()).isFalse();
        assertSoftly(softly -> {
            softly.assertThat(p.getExpectedPageSize()).isEqualTo(pageSize);
            softly.assertThat(p.getPageId()).isEqualTo(1);
            softly.assertThat(p.getTotalItemCount()).isEqualTo(1);
            softly.assertThat(p.getItemsOnPage()).hasSize(0);
        });
    }
}
