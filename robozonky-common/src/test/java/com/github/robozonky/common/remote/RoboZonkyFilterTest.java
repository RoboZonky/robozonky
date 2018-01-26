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

import java.io.IOException;
import java.util.Collections;
import java.util.UUID;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import com.github.robozonky.internal.api.Defaults;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.assertj.core.api.SoftAssertions.*;
import static org.mockito.Mockito.*;

class RoboZonkyFilterTest {

    @Test
    void userAgent() throws IOException {
        final MultivaluedMap<String, Object> map = new MultivaluedMapImpl<>();
        final ClientRequestContext ctx = mock(ClientRequestContext.class);
        when(ctx.getHeaders()).thenReturn(map);
        new RoboZonkyFilter().filter(ctx);
        assertThat(map.get("User-Agent").get(0)).isEqualTo(Defaults.ROBOZONKY_USER_AGENT);
    }

    @Test
    void response() throws IOException {
        final String key = UUID.randomUUID().toString();
        final String key2 = UUID.randomUUID().toString();
        final String value = UUID.randomUUID().toString();
        final MultivaluedMap<String, String> map = new MultivaluedMapImpl<>();
        map.add(key, value);
        map.addAll(key2, Collections.emptyList());
        final ClientRequestContext ctx = mock(ClientRequestContext.class);
        final ClientResponseContext ctx2 = mock(ClientResponseContext.class);
        when(ctx2.getHeaders()).thenReturn(map);
        when(ctx2.getStatusInfo()).thenReturn(mock(Response.StatusType.class));
        final RoboZonkyFilter filter = new RoboZonkyFilter();
        filter.filter(ctx, ctx2);
        assertSoftly(softly -> {
            softly.assertThat(filter.getLastResponseHeader(key)).contains(value);
            softly.assertThat(filter.getLastResponseHeader(key2)).isEmpty();
        });
    }
}
