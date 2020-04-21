/*
 * Copyright 2020 The RoboZonky Project
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

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Consumer;
import java.util.function.Function;

import javax.ws.rs.ProcessingException;

import org.junit.jupiter.api.Test;

import com.github.robozonky.internal.remote.endpoints.LoanApi;

class ApiTest {

    private final RequestCounter counter = new RequestCounterImpl();

    @Test
    void executeFunction() {
        final LoanApi mock = mock(LoanApi.class);
        final Api<LoanApi> api = new Api<>(mock, counter);
        final String expected = UUID.randomUUID()
            .toString();
        final Function<LoanApi, String> function = (a) -> expected;
        final String result = api.call(function);
        assertThat(result).isSameAs(expected);
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void failsImmediately() {
        final LoanApi mock = mock(LoanApi.class);
        final Api<LoanApi> api = new Api<>(mock, counter);
        final Function<LoanApi, String> function = (a) -> {
            throw new IllegalStateException();
        };
        assertThatThrownBy(() -> api.call(function)).isInstanceOf(IllegalStateException.class);
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void failsAndDoesNotRetry() {
        final LoanApi mock = mock(LoanApi.class);
        final Api<LoanApi> api = new Api<>(mock, counter);
        final Function<LoanApi, String> function = (a) -> {
            throw new ProcessingException(new IllegalStateException());
        };
        assertThatThrownBy(() -> api.call(function))
            .isInstanceOf(ProcessingException.class)
            .hasCauseInstanceOf(ProcessingException.class);
        assertThat(counter.count()).isEqualTo(1);
    }

    @Test
    void retriesAfterTimeout() {
        final LoanApi mock = mock(LoanApi.class);
        final Api<LoanApi> api = new Api<>(mock);
        final String expected = UUID.randomUUID()
            .toString();
        final LongAdder adder = new LongAdder();
        final Function<LoanApi, String> function = (a) -> {
            adder.add(1);
            if (adder.intValue() > 2) {
                return expected;
            } else if (adder.intValue() == 2) {
                throw new ProcessingException(new IOException());
            } else {
                throw new ProcessingException(new SocketTimeoutException());
            }
        };
        final String result = api.call(function);
        assertThat(result).isSameAs(expected);
    }

    @Test
    void executeProcedure() {
        final LoanApi mock = mock(LoanApi.class);
        final Api<LoanApi> api = new Api<>(mock, counter);
        final Consumer<LoanApi> procedure = mock(Consumer.class);
        api.run(procedure);
        verify(procedure, times(1)).accept(eq(mock));
        assertThat(counter.count()).isEqualTo(1);
    }
}
