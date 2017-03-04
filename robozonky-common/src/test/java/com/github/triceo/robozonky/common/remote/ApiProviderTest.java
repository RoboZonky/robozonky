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

import java.util.List;
import java.util.function.Function;

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import com.github.triceo.robozonky.api.remote.entities.Loan;
import com.github.triceo.robozonky.api.remote.entities.ZonkyApiToken;
import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.Mockito;

public class ApiProviderTest {

    @Test
    public void authenticated() {
        ApiProvider.ApiWrapper<ZonkyApi> api;
        try (final ApiProvider provider = new ApiProvider()) {
            api = provider.authenticated(Mockito.mock(ZonkyApiToken.class));
            Assertions.assertThat(api).isNotNull();
        }
        Assertions.assertThatThrownBy(() -> api.execute((Function<ZonkyApi, List<Loan>>) ZonkyApi::getLoans))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void standard() {
        try (final ApiProvider provider = new ApiProvider()) {
            Assertions.assertThat(provider.anonymous()).isNotNull();
        }
    }

    @Test
    public void oauth() {
        try (final ApiProvider provider = new ApiProvider()) {
            Assertions.assertThat(provider.oauth()).isNotNull();
        }
    }

    @Test
    public void obtainClosedThrows() {  // tests double-closing as a side-effect
        try (final ApiProvider provider = new ApiProvider()) {
            try (final ApiProvider.ApiWrapper<ZonkyApi> w = provider.anonymous()) {
                Assertions.assertThat(w.isClosed()).isFalse();
                provider.close();
                Assertions.assertThat(w.isClosed()).isTrue();
            }
            Assertions.assertThatThrownBy(provider::anonymous).isInstanceOf(IllegalStateException.class);
        }
    }

}
