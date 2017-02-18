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

package com.github.triceo.robozonky.internal.api;

import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.Api;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

public class ApiWrapperTest {

    @Test
    public void executeFunction() {
        final Api mock = Mockito.mock(Api.class);
        final AbstractApiProvider.ApiWrapper<Api> wrapper = new AbstractApiProvider.ApiWrapper<>(mock);
        final String expected = UUID.randomUUID().toString();
        final Function<Api, String> function = api -> expected;
        final String result = wrapper.execute(function);
        Assertions.assertThat(result).isSameAs(expected);
    }

    @Test
    public void executeProcedure() {
        final Api mock = Mockito.mock(Api.class);
        final AbstractApiProvider.ApiWrapper<Api> wrapper = new AbstractApiProvider.ApiWrapper<>(mock);
        final Consumer<Api> procedure = Mockito.mock(Consumer.class);
        wrapper.execute(procedure);
        Mockito.verify(procedure, Mockito.times(1)).accept(ArgumentMatchers.eq(mock));
    }

}
