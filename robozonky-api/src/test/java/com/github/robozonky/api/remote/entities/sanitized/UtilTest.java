/*
 * Copyright 2018 The RoboZonky Project
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
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UtilTest {

    @Test
    void guessesUrl() {
        final int id = 1;
        final RawLoan mocked = mock(RawLoan.class);
        when(mocked.getId()).thenReturn(id);
        assertThat(Util.getUrlSafe(mocked).toString()).isEqualTo(
                "https://app.zonky.cz/#/marketplace/detail/" + id + "/");
    }

    @Test
    void hasUrl() {
        final RawLoan mocked = mock(RawLoan.class);
        when(mocked.getUrl()).thenReturn("http://something");
        assertThat(Util.getUrlSafe(mocked).toString()).isEqualTo(mocked.getUrl());
    }

    @Test
    void hasWrongUrl() {
        final RawLoan mocked = mock(RawLoan.class);
        when(mocked.getUrl()).thenReturn("something");
        assertThatThrownBy(() -> Util.getUrlSafe(mocked)).isInstanceOf(IllegalStateException.class);
    }
}
