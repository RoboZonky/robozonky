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

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class SortTest {

    @Test
    public void unspecified() {
        final RoboZonkyFilter filter = Mockito.mock(RoboZonkyFilter.class);
        Sort.unspecified().apply(filter);
        Mockito.verify(filter, Mockito.times(0))
                .setRequestHeader(ArgumentMatchers.any(), ArgumentMatchers.any());
    }

    @Test
    public void simple() {
        final RoboZonkyFilter filter = Mockito.mock(RoboZonkyFilter.class);
        Sort.by(LoanField.COVERED).apply(filter);
        Mockito.verify(filter, Mockito.times(1))
                .setRequestHeader(ArgumentMatchers.eq("X-Order"), ArgumentMatchers.eq("covered"));
    }

    @Test
    public void simpleDescending() {
        final RoboZonkyFilter filter = Mockito.mock(RoboZonkyFilter.class);
        Sort.by(LoanField.COVERED, false).apply(filter);
        Mockito.verify(filter, Mockito.times(1))
                .setRequestHeader(ArgumentMatchers.eq("X-Order"), ArgumentMatchers.eq("-covered"));
    }

    @Test
    public void complex() {
        final RoboZonkyFilter filter = Mockito.mock(RoboZonkyFilter.class);
        Sort.by(LoanField.COVERED).thenBy(LoanField.DATE_PUBLISHED).thenBy(LoanField.INTEREST_RATE, false)
                .apply(filter);
        Mockito.verify(filter, Mockito.times(1))
                .setRequestHeader(ArgumentMatchers.eq("X-Order"),
                                  ArgumentMatchers.eq("covered,datePublished,-interestRate"));
    }

    @Test
    public void orderTwiceOnSameField() {
        Assertions.assertThatThrownBy(() -> Sort.by(LoanField.COVERED).thenBy(LoanField.COVERED))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
