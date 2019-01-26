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

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.robozonky.api.remote.LoanApi;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiTest {

    @Mock
    private Consumer<LoanApi> procedure;

    @Test
    void executeFunction() {
        final LoanApi mock = mock(LoanApi.class);
        final Api<LoanApi> api = new Api<>(mock);
        final String expected = UUID.randomUUID().toString();
        final Function<LoanApi, String> function = (a) -> expected;
        final String result = api.call(function);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void executeProcedure() {
        final LoanApi mock = mock(LoanApi.class);
        final Api<LoanApi> api = new Api<>(mock);
        api.run(procedure);
        verify(procedure, times(1)).accept(eq(mock));
    }
}
