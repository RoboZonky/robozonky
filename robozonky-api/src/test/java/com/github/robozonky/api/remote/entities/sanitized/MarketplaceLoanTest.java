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

package com.github.robozonky.api.remote.entities.sanitized;

import com.github.robozonky.api.remote.entities.RawLoan;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class MarketplaceLoanTest {

    @Mock
    private RawLoan mocked;

    @Test
    @DisplayName("Sanitization works.")
    void sanitizing() {
        assertThat(MarketplaceLoan.sanitized(mocked)).isNotNull();
    }

    @Test
    @DisplayName("Custom loan works.")
    void custom() {
        assertThat(MarketplaceLoan.custom().build()).isNotNull();
    }

    @Test
    void hasToString() {
        assertThat(MarketplaceLoan.custom().build().toString()).isNotEmpty();
    }

}
