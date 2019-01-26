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
import java.util.function.Function;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiProviderTest {

    private static ZonkyApiToken mockToken() {
        final ZonkyApiToken t = mock(ZonkyApiToken.class);
        when(t.getAccessToken()).thenReturn(UUID.randomUUID().toString().toCharArray());
        return t;
    }

    @Test
    void unauthenticatedProperlyClosed() {
        final ApiProvider p = spy(new ApiProvider());
        try (final ApiProvider provider = p) {
            final OAuth result = provider.oauth(Function.identity());
            assertThat(result).isNotNull();
        }
        verify(p).close();
        assertThat(p.isClosed()).isTrue();
    }

    @Test
    void authenticatedProperlyClosed() {
        final ApiProvider p = spy(new ApiProvider());
        try (final ApiProvider provider = p) {
            final Zonky result = provider.call(Function.identity(), ApiProviderTest::mockToken);
            assertThat(result).isNotNull();
        }
        verify(p).close();
        assertThat(p.isClosed()).isTrue();
    }

    @Test
    void marketplace() {
        final ApiProvider p = spy(new ApiProvider());
        try (final ApiProvider provider = p) {
            final Zonky result = provider.call(Function.identity(), ApiProviderTest::mockToken);
            assertThat(result).isNotNull();
        }
        verify(p).close();
        assertThat(p.isClosed()).isTrue();
    }

}
