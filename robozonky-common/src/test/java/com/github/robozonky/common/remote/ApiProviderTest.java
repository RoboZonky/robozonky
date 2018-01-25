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

import java.util.function.Function;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class ApiProviderTest {

    private static final Function<OAuth, OAuth> AUTH_OPERATION = Mockito::spy;
    private static final Function<Zonky, Zonky> ZONKY_OPERATION = Mockito::spy;
    private static final char[] PASSWORD = new char[0];

    @Test
    public void unathenticatedApisProperlyClosed() {
        final ApiProvider provider = new ApiProvider();
        Assertions.assertThat(provider.marketplace()).isNotNull();
        final OAuth spy = provider.oauth(AUTH_OPERATION);
        Assertions.assertThatThrownBy(() -> spy.login("", PASSWORD)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    public void authenticatedApisProperlyClosed() {
        final ZonkyApiToken token = AuthenticatedFilterTest.TOKEN;
        final ApiProvider provider = new ApiProvider();
        final Zonky spy = provider.authenticated(token, ZONKY_OPERATION);
        Assertions.assertThatThrownBy(spy::logout).isInstanceOf(IllegalStateException.class);

    }
}
