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

package com.github.triceo.robozonky;

import com.github.triceo.robozonky.api.remote.Api;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ApiProviderTest {

    @Test(expected = IllegalStateException.class)
    public void authenticated() {
        Api api;
        try (final ApiProvider provider = new ApiProvider()) {
            api = provider.authenticated(Mockito.mock(ZonkyApiToken.class));
            Assertions.assertThat(api).isNotNull();
        }
        api.getLoans();
    }

    @Test(expected = IllegalStateException.class)
    public void cache() {
        Api api;
        try (final ApiProvider provider = new ApiProvider()) {
            api = provider.cache();
            Assertions.assertThat(api).isNotNull();
        }
        api.getLoans();
    }

    @Test
    public void doubleClosedNoException() {
        final ApiProvider provider = new ApiProvider();
        provider.close();
        provider.close();
    }

    @Test(expected = IllegalStateException.class)
    public void obtainClosedThrows() {
        final ApiProvider provider = new ApiProvider();
        provider.close();
        provider.oauth();
    }

}
