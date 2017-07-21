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

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.LoanApi;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ApiTest {

    @Test
    public void executeFunction() {
        final LoanApi mock = Mockito.mock(LoanApi.class);
        final Api<LoanApi> api = new Api<>(mock);
        final String expected = UUID.randomUUID().toString();
        final Function<LoanApi, String> function = (a) -> expected;
        final String result = api.execute(function);
        Assertions.assertThat(result).isSameAs(expected);
    }

    @Test
    public void executeProcedure() {
        final LoanApi mock = Mockito.mock(LoanApi.class);
        final Api<LoanApi> api = new Api<>(mock);
        final Consumer<LoanApi> procedure = Mockito.mock(Consumer.class);
        api.execute(procedure);
        Mockito.verify(procedure, Mockito.times(1)).accept(ArgumentMatchers.eq(mock));
    }
}
