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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.UUID;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.Response;

import com.github.robozonky.api.remote.entities.ZonkyApiToken;
import com.github.robozonky.internal.api.Defaults;
import org.assertj.core.api.Assertions;
import org.jboss.resteasy.specimpl.MultivaluedMapImpl;
import org.junit.Test;
import org.mockito.Mockito;

public class AuthenticatedFilterTest extends AbstractCommonFilterTest {

    static final ZonkyApiToken TOKEN = new ZonkyApiToken(UUID.randomUUID().toString(),
                                                         UUID.randomUUID().toString(), 300);

    private static InputStream c(final ZonkyApiToken token) {
        final String error = "{\"error\":\"invalid_token\",\"error_description\":\"Invalid access token: "
                + Arrays.toString(token.getAccessToken()) + "\"}";
        return new ByteArrayInputStream(error.getBytes(Defaults.CHARSET));
    }

    @Override
    protected RoboZonkyFilter getTestedFilter() {
        return new AuthenticatedFilter(AuthenticatedFilterTest.TOKEN);
    }

    @Test
    public void hasToken() throws IOException {
        final ClientRequestContext crc = Mockito.mock(ClientRequestContext.class);
        Mockito.when(crc.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        getTestedFilter().filter(crc);
        Assertions.assertThat(crc.getHeaders().getFirst("Authorization"))
                .isEqualTo("Bearer " + String.valueOf(AuthenticatedFilterTest.TOKEN.getAccessToken()));
    }

    @Test
    public void changes400to401() throws IOException {
        final int expectedCode = 400;
        final ClientRequestContext ctx = Mockito.mock(ClientRequestContext.class);
        final ClientResponseContext ctx2 = Mockito.mock(ClientResponseContext.class);
        final ZonkyApiToken token = new ZonkyApiToken("", "", 299);
        Mockito.when(ctx2.hasEntity()).thenReturn(true);
        Mockito.when(ctx2.getHeaders()).thenReturn(new MultivaluedMapImpl<>());
        Mockito.when(ctx2.getEntityStream()).thenReturn(c(token));
        Mockito.when(ctx2.getStatusInfo()).thenReturn(Response.Status.fromStatusCode(expectedCode));
        Mockito.when(ctx2.getStatus()).thenReturn(expectedCode);
        final RoboZonkyFilter filter = new AuthenticatedFilter(token);
        filter.filter(ctx, ctx2);
        Mockito.verify(ctx2, Mockito.times(1)).setStatus(401);
    }
}
