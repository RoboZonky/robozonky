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

import com.github.triceo.robozonky.api.remote.ZonkyApi;
import org.assertj.core.api.Assertions;
import org.junit.Test;

public class AbstractApiProviderTest {

    private static final class ApiProvider extends AbstractApiProvider {

        public AbstractApiProvider.ApiWrapper<ZonkyApi> getApi() {
            return this.obtain(ZonkyApi.class, "https://api.zonky.cz", new RoboZonkyFilter());
        }

    }

    @Test
    public void standard() {
        try (final AbstractApiProviderTest.ApiProvider provider = new AbstractApiProviderTest.ApiProvider()) {
            Assertions.assertThat(provider.getApi()).isNotNull();
        }
    }

    @Test
    public void obtainClosedThrows() {  // tests double-closing as a side-effect
        try (final AbstractApiProviderTest.ApiProvider provider = new AbstractApiProviderTest.ApiProvider()) {
            try (final AbstractApiProvider.ApiWrapper<ZonkyApi> w = provider.getApi()) {
                Assertions.assertThat(w.isClosed()).isFalse();
                provider.close();
                Assertions.assertThat(w.isClosed()).isTrue();
            }
            Assertions.assertThatThrownBy(provider::getApi).isInstanceOf(IllegalStateException.class);
        }
    }

}
