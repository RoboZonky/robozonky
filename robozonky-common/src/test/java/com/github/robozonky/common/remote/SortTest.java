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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class SortTest {

    @Test
    void unspecified() {
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        Sort.unspecified().apply(filter);
        verify(filter, times(0))
                .setRequestHeader(any(), any());
    }

    @Test
    void simple() {
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        Sort.by(LoanField.COVERED).apply(filter);
        verify(filter, times(1))
                .setRequestHeader(eq("X-Order"), eq("covered"));
    }

    @Test
    void simpleDescending() {
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        Sort.by(LoanField.COVERED, false).apply(filter);
        verify(filter, times(1))
                .setRequestHeader(eq("X-Order"), eq("-covered"));
    }

    @Test
    void complex() {
        final RoboZonkyFilter filter = mock(RoboZonkyFilter.class);
        Sort.by(LoanField.COVERED).thenBy(LoanField.DATE_PUBLISHED).thenBy(LoanField.INTEREST_RATE, false)
                .apply(filter);
        verify(filter, times(1))
                .setRequestHeader(eq("X-Order"),
                                  eq("covered,datePublished,-interestRate"));
    }

    @Test
    void orderTwiceOnSameField() {
        assertThatThrownBy(() -> Sort.by(LoanField.COVERED).thenBy(LoanField.COVERED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
