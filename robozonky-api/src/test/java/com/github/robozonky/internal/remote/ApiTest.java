/*
 * Copyright 2021 The RoboZonky Project
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

package com.github.robozonky.internal.remote;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.remote.endpoints.LoanApi;

class ApiTest {

    @Test
    void executeFunction() {
        var mock = mock(LoanApi.class);
        var api = new Api<>(mock);
        var expected = UUID.randomUUID()
            .toString();
        Function<LoanApi, String> function = (a) -> expected;
        var result = api.call(function);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void failsImmediately() {
        var mock = mock(LoanApi.class);
        var api = new Api<>(mock);
        Function<LoanApi, String> function = (a) -> {
            throw new IllegalStateException();
        };
        assertThatThrownBy(() -> api.call(function)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void executeProcedure() {
        var mock = mock(LoanApi.class);
        var api = new Api<>(mock);
        Consumer<LoanApi> procedure = mock(Consumer.class);
        api.run(procedure);
        verify(procedure, times(1)).accept(eq(mock));
    }
}
