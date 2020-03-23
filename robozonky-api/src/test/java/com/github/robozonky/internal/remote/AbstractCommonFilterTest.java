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

import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.Test;

abstract class AbstractCommonFilterTest {

    protected abstract RoboZonkyFilter getTestedFilter();

    protected static ClientRequestContext mockClientRequestContext() throws URISyntaxException {
        final MultivaluedMap<String, Object> map = mock(MultivaluedMap.class);
        final ClientRequestContext ctx = mock(ClientRequestContext.class);
        when(ctx.getHeaders()).thenReturn(map);
        when(ctx.getUri()).thenReturn(new URI("http://localhost"));
        return ctx;
    }

    protected static ClientResponseContext mockClientResponseContext(Map<String, Object> headers) {
        final MultivaluedMap<String, String> map = mock(MultivaluedMap.class);
        final Map<String, List<String>> result = new HashMap<>();
        headers.forEach((k, v) -> {
            if (v instanceof Collection) {
                var strings = ((Collection<Object>) v).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
                result.put(k, strings);
            } else {
                result.put(k, Collections.singletonList(v.toString()));
            }
        });
        when(map.entrySet()).thenReturn(result.entrySet());
        final ClientResponseContext ctx = mock(ClientResponseContext.class);
        when(ctx.getHeaders()).thenReturn(map);
        return ctx;
    }

    protected static ClientResponseContext mockClientResponseContext() {
        return mockClientResponseContext(Collections.emptyMap());
    }

    @Test
    void request() throws URISyntaxException {
        final ClientRequestContext ctx = mockClientRequestContext();
        final RoboZonkyFilter filter = getTestedFilter();
        filter.setQueryParam("something", "value");
        filter.filter(ctx);
        verify(ctx).setUri(new URI("http://localhost?something=value"));
    }

    @Test
    void response() throws IOException, URISyntaxException {
        final String key = UUID.randomUUID()
            .toString();
        final String key2 = UUID.randomUUID()
            .toString();
        final String value = UUID.randomUUID()
            .toString();
        final Map<String, Object> headers = Map.of(key, value, key2, Collections.emptyList());
        final ClientResponseContext ctx2 = mockClientResponseContext(headers);
        when(ctx2.getStatusInfo()).thenReturn(mock(Response.StatusType.class));
        final RoboZonkyFilter filter = getTestedFilter();
        filter.filter(mockClientRequestContext(), ctx2);
        assertSoftly(softly -> {
            softly.assertThat(filter.getLastResponseHeader(key))
                .contains(value);
            softly.assertThat(filter.getLastResponseHeader(key2))
                .isEmpty();
        });
    }

}
