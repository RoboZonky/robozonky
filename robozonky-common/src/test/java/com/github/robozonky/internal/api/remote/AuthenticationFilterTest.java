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

package com.github.robozonky.internal.api.remote;

import java.net.URI;
import java.net.URISyntaxException;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.core.MultivaluedHashMap;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthenticationFilterTest extends AbstractCommonFilterTest {

    @Override
    protected RoboZonkyFilter getTestedFilter() {
        return new AuthenticationFilter();
    }

    @Test
    void wasAuthorizationAdded() throws URISyntaxException {
        final ClientRequestContext crc = mock(ClientRequestContext.class);
        when(crc.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(crc.getUri()).thenReturn(new URI("http://somewhere"));
        this.getTestedFilter().filter(crc);
        assertThat(crc.getHeaders()).containsKey("Authorization");
    }
}
